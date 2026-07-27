package configgen.value;

import configgen.data.CfgData.DCell;
import configgen.data.CfgData.DTable;
import configgen.schema.FieldFormat;
import configgen.schema.FieldSchema;
import configgen.schema.FieldType;
import configgen.schema.Fieldable;
import configgen.schema.InterfaceSchema;
import configgen.schema.Span;
import configgen.schema.StructSchema;
import configgen.schema.Structural;
import configgen.schema.TableSchema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static configgen.value.ValueParser.BlockParser;
import static configgen.value.ValueParser.CellsWithRowIndex;

/**
 * block 解析算法（8aa463fc 起）：用「词法包围本 block 的所有祖先 block 首列」判定嵌套边界，
 * 任一祖先首列非空 => 外层 block 起了新项 => 结束本 block。
 * <p>
 * 从 VTableParser 抽出，使 VTableParser 退化为纯 parseTable 驱动，并允许迁移工具注入其它 BlockParser
 * （如 {@link ComparingBlockParser}）做新旧算法对比。列号空间、递归范围与
 * {@code BlockFirstColOverlapChecker} 保持一致（只对 Block 格式的 list/map 字段展开元素）。
 */
public class VTableBlockParser implements BlockParser {
    /** 一个 block 字段的预算信息：祖先首列集合（解析用）+ 字段名（迁移报告用）。 */
    private record BlockFieldInfo(SortedSet<Integer> ancestors, String fieldName) {
        static final BlockFieldInfo EMPTY =
                new BlockFieldInfo(Collections.emptySortedSet(), "");
    }

    private final DTable dTable;
    private final List<Integer> pkColumnIndices;
    private final Map<Integer, BlockFieldInfo> blockFirstColToInfo;

    public VTableBlockParser(DTable dTable, TableSchema tableSchema) {
        this.dTable = dTable;
        this.pkColumnIndices = VTableParser.getPkColumnIndices(tableSchema);
        this.blockFirstColToInfo = collectBlockAncestors(tableSchema);
    }

    // 要允许 block<struct>，struct 里仍然有 block，如下所示
    //   xxxaabbxccc
    //        bb ccc
    //        bb
    //      aabb
    //        bb
    // aabb 前面一列要有空格，bb 前一列格子也要是空，ccc 前一列也是有个空，
    // 用这个空来做为标记，支持 block aabb 嵌套 block bb，来判断此行 bb 是否属于嵌套的 bb 还是新起的 aabb。
    //
    // 历史算法（≤8aa463fc^，见 ComparingBlockParser）用 firstColIndex-1 这一列是否为空判断嵌套边界。但当外层
    // block 的元素 struct 还有其它兄弟 primitive 字段时（如 levelmonster 的 waves<LevelWave>，LevelWave.startTime
    // 紧挨在 spawns block 前面），firstColIndex-1 会落在那个兄弟字段上，而不是外层 block 首列；策划若在内层
    // block 行冗余填了该兄弟字段，就会被误判为"外层起新项"而提前 break，丢失后续元素。改为检查"词法包围本
    // block 的所有祖先 block 首列"（blockFirstColToInfo 按 schema 预算）：任一祖先首列非空 => 外层 block
    // 起了新项 => 结束本 block。
    @Override
    public List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex) {
        DCell firstCell = cells.getFirst();
        int rowSize = dTable.rows().size();
        List<DCell> curRow = dTable.rows().get(curRowIndex);
        int firstColIndex = findColumnIndex(firstCell, curRow);

        int colSize = cells.size();

        SortedSet<Integer> ancestors =
                blockFirstColToInfo.getOrDefault(firstColIndex, BlockFieldInfo.EMPTY).ancestors();

        List<CellsWithRowIndex> res = new ArrayList<>();
        res.add(new CellsWithRowIndex(cells, curRowIndex));

        for (int row = curRowIndex + 1; row < rowSize; row++) {
            List<DCell> line = dTable.rows().get(row);

            if (VTableParser.isPkCellAllEmpty(line, pkColumnIndices)) {  // 主键所在格子为空，还是本 record
                boolean newOuterItem = false;
                for (int bc : ancestors) {
                    if (!line.get(bc).isCellEmpty()) { // 任一祖先 block 首列非空 => 外层起一新项
                        newOuterItem = true;
                        break;
                    }
                }
                if (newOuterItem) {
                    break;
                }

                DCell thisCell = line.get(firstColIndex);
                //noinspection StatementWithEmptyBody
                if (thisCell.isCellEmpty()) { // 本格为空，内部的嵌套 block，忽略
                } else { // 本格不为空 -> 是这个 block 了
                    res.add(new CellsWithRowIndex(line.subList(firstColIndex, firstColIndex + colSize), row));
                }
            } else { // 下一个 record，结束
                break;
            }
        }

        return res;
    }

    /** 当前 block 字段的名称（迁移报告用）。firstColIndex 未命中返回空串。 */
    String fieldNameOf(int firstColIndex) {
        return blockFirstColToInfo.getOrDefault(firstColIndex, BlockFieldInfo.EMPTY).fieldName();
    }

    /**
     * 预计算每个 block 字段首列(cell-list index) -> {祖先 block 首列集合, 字段名}。
     * 列号空间与 parseBlock 的 firstColIndex 一致：都是 fieldIndices 过滤后的 cell list index，
     * 用 Span.fieldSpan 累加得到。模仿 Span.calcFieldSpanCheckLoop 的递归风格。
     * <p>
     * 键唯一性假设：用首列号做 key 隐含"一个列号唯一标识一个 block"。若内层 block 首列与某外层祖先
     * block 首列重合（最典型：内层 block 排在外层 block 元素 struct 的首字段位置），下方 result.put 会用
     * 同一 startCol 覆盖外层条目，parseBlock 反查时就拿到错误的信息。该缺陷现由
     * {@code BlockFirstColOverlapChecker} 在 schema 阶段拒绝首列重合而规避——违反者进不到解析阶段，故此处对
     * 覆盖不做防御；若未来放宽该校验，需同步重审此处的 key 唯一性。
     */
    private static Map<Integer, BlockFieldInfo> collectBlockAncestors(Structural structural) {
        Map<Integer, BlockFieldInfo> result = new HashMap<>();
        collectStructuralBlockAncestors(structural, 0, Collections.emptySortedSet(), result);
        return result;
    }

    private static void collectStructuralBlockAncestors(Structural structural, int startCol,
                                                        SortedSet<Integer> outerAncestors,
                                                        Map<Integer, BlockFieldInfo> result) {
        // pack/sep 结构只占 1 列，内部不可能有 block（Span 计算时已保证占 1 列）
        FieldFormat fmt = structural.fmt();
        if (fmt == FieldFormat.AutoOrPack.PACK || fmt instanceof FieldFormat.Sep) {
            return;
        }
        int col = startCol;
        for (FieldSchema field : structural.fields()) {
            collectFieldBlockAncestors(field, col, outerAncestors, result);
            col += Span.fieldSpan(field);
        }
    }

    private static void collectFieldBlockAncestors(FieldSchema field, int startCol,
                                                   SortedSet<Integer> outerAncestors,
                                                   Map<Integer, BlockFieldInfo> result) {
        // pack/sep 字段只占 1 列，内部结构不展开（Span 也不为它们计算内部 span），不可能含 block。
        // 这也是断开 struct/interface 自引用环的关键：合法 schema 中自引用必须经 pack 截断
        // （否则 Span 计算会报 StructNestLoop），在 pack 处 return 即终止递归。
        FieldFormat fmt = field.fmt();
        if (fmt == FieldFormat.AutoOrPack.PACK || fmt instanceof FieldFormat.Sep) {
            return;
        }
        switch (field.type()) {
            case FieldType.Primitive ignored -> { }
            case FieldType.StructRef sr ->
                    collectFieldableBlockAncestors(sr.obj(), startCol, outerAncestors, result);
            case FieldType.FList fl -> {
                if (field.fmt() instanceof FieldFormat.Block(int fix)) {
                    result.put(startCol, new BlockFieldInfo(outerAncestors, field.name()));
                    // 元素内 block 的祖先 = 外层祖先 + 本 block 首列；
                    // fix>1 时横排的每个元素副本互为兄弟（互不为祖先），各自独立传入同一 itemAncestors
                    SortedSet<Integer> itemAncestors = concatSorted(outerAncestors, startCol);
                    int itemSpan = Span.simpleTypeSpan(fl.item());
                    for (int i = 0; i < fix; i++) {
                        collectSimpleTypeBlockAncestors(fl.item(), startCol + i * itemSpan, itemAncestors, result);
                    }
                }
            }
            case FieldType.FMap fm -> {
                if (field.fmt() instanceof FieldFormat.Block(int fix)) {
                    result.put(startCol, new BlockFieldInfo(outerAncestors, field.name()));
                    SortedSet<Integer> entryAncestors = concatSorted(outerAncestors, startCol);
                    int keySpan = Span.simpleTypeSpan(fm.key());
                    int entrySpan = keySpan + Span.simpleTypeSpan(fm.value());
                    // key 和 value 同属一个 entry，互为兄弟（互不为祖先）
                    for (int i = 0; i < fix; i++) {
                        int entryCol = startCol + i * entrySpan;
                        collectSimpleTypeBlockAncestors(fm.key(), entryCol, entryAncestors, result);
                        collectSimpleTypeBlockAncestors(fm.value(), entryCol + keySpan, entryAncestors, result);
                    }
                }
            }
        }
    }

    private static void collectFieldableBlockAncestors(Fieldable fieldable, int startCol,
                                                       SortedSet<Integer> outerAncestors,
                                                       Map<Integer, BlockFieldInfo> result) {
        switch (fieldable) {
            case Structural s ->
                    collectStructuralBlockAncestors(s, startCol, outerAncestors, result);
            case InterfaceSchema is -> {
                // interface 第一列是 impl 名（在 startCol），各 impl 字段从 startCol+1 起（Span 用 max+1）
                for (StructSchema impl : is.impls()) {
                    collectStructuralBlockAncestors(impl, startCol + 1, outerAncestors, result);
                }
            }
        }
    }

    private static void collectSimpleTypeBlockAncestors(FieldType.SimpleType st, int startCol,
                                                        SortedSet<Integer> outerAncestors,
                                                        Map<Integer, BlockFieldInfo> result) {
        switch (st) {
            case FieldType.Primitive ignored -> { }
            case FieldType.StructRef sr ->
                    collectFieldableBlockAncestors(sr.obj(), startCol, outerAncestors, result);
        }
    }

    private static SortedSet<Integer> concatSorted(SortedSet<Integer> base, int add) {
        TreeSet<Integer> r = new TreeSet<>(base);
        r.add(add);
        return Collections.unmodifiableSortedSet(r);
    }


    /**
     * 在 curRow（整行 cell list）里定位 cell 的列号（cell-list index）。
     * 供 {@link ComparingBlockParser} 旧算法复用。
     */
    static int findColumnIndex(DCell cell, List<DCell> curRow) {
        int i = 0;
        for (DCell c : curRow) {
            if (c.col() == cell.col()) {
                return i;
            }
            i++;
        }
        return i;
    }
}
