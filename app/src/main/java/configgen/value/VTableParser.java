package configgen.value;

import configgen.ctx.HeadRow;
import configgen.schema.FieldFormat;
import configgen.schema.FieldSchema;
import configgen.schema.FieldType;
import configgen.schema.Fieldable;
import configgen.schema.HasBlock;
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

import static configgen.data.CfgData.DCell;
import static configgen.data.CfgData.DTable;
import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;

import static configgen.value.ValueParser.BlockParser;
import static configgen.value.ValueParser.CellsWithRowIndex;

public class VTableParser implements BlockParser {
    private final TableSchema subTableSchema;
    private final DTable dTable;
    private final TableSchema tableSchema;
    private final CfgValueErrs errs;
    private final ValueParser parser;
    private final List<Integer> pkColumnIndices;
    private final Map<Integer, SortedSet<Integer>> blockFirstColToAncestors;
    private List<DCell> curRow;

    public VTableParser(TableSchema subTableSchema,
                        DTable dTable,
                        TableSchema tableSchema,
                        HeadRow headRow,
                        CfgValueErrs errs) {
        this.subTableSchema = subTableSchema;
        this.dTable = dTable;
        this.tableSchema = tableSchema;
        this.errs = errs;
        this.parser = new ValueParser(errs, headRow, this);
        this.pkColumnIndices = getPkColumnIndices(tableSchema);
        this.blockFirstColToAncestors = collectBlockAncestors(tableSchema);
    }

    public VTable parseTable() {
        boolean hasBlock = HasBlock.hasBlock(tableSchema);

        int rowCnt = dTable.rows().size();
        List<VStruct> valueList = new ArrayList<>(); //可能会多，无所谓
        for (int curRecordRow = 0; curRecordRow < rowCnt; ) {
            curRow = dTable.rows().get(curRecordRow);
            VStruct vStruct = parser.parseStructural(subTableSchema, curRow, tableSchema,
                    new ValueParser.ParseContext(tableSchema.fullName(), false, true, curRecordRow));
            if (vStruct != null) {
                valueList.add(vStruct);
            }
            curRecordRow++;

            if (hasBlock) {
                while (curRecordRow < rowCnt) {
                    List<DCell> nr = dTable.rows().get(curRecordRow);
                    // 用主键所在格子是否全为空  来判断这行是属于上一个record的block，还是新的一格record
                    if (isPkCellAllEmpty(nr)) {
                        curRecordRow++;  // 具体提取让VList，VMap，通郭parseBlock自己去提取
                    } else {
                        break;
                    }
                }
            }
        }

        return new VTableCreator(subTableSchema, errs).create(valueList);
    }


    // 主键所在格子是否全为空
    private boolean isPkCellAllEmpty(List<DCell> row) {
        for (Integer pkIndex : pkColumnIndices) {
            DCell dCell = row.get(pkIndex);
            if (!dCell.value().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // 要允许block<struct>,struct里仍然有block，如下所示
    // xxxaabbxccc
    //      bb ccc
    //      bb
    //    aabb
    //      bb
    // aabb前面一列要有空格，bb前一列格子也要是空，ccc前一列也是有个空，
    // 用这个空来做为标记，支持block aabb嵌套block bb，来判断此行bb是否属于嵌套的bb还是新起的aabb
    // 这样也强制了2个同级的block不要直接衔接，视觉上不好区分，
    // 可以在中间加入一个列，比如以上的aabb和ccc直接有x来分割
    // 以上规则现在没有做检测，要检测有点复杂，人工保证吧。
    //
    // 历史算法用 firstColIndex-1 这一列是否为空判断嵌套边界。但当外层 block 的元素 struct
    // 还有其它兄弟 primitive 字段时（如 levelmonster 的 waves<LevelWave>，LevelWave.startTime
    // 紧挨在 spawns block 前面），firstColIndex-1 会落在那个兄弟字段上，而不是外层 block 首列；
    // 策划若在内层 block 行冗余填了该兄弟字段，就会被误判为“外层起新项”而提前 break，
    // 丢失后续元素。改为检查“词法包围本 block 的所有祖先 block 首列”（blockFirstColToAncestors
    // 按 schema 预算）：任一祖先首列非空 => 外层 block 起了新项 => 结束本 block。
    @Override
    public List<CellsWithRowIndex> parseBlock(List<DCell> cells, int curRowIndex) {
        DCell firstCell = cells.getFirst();
        int rowSize = dTable.rows().size();
        int firstColIndex = findColumnIndex(firstCell);

        int colSize = cells.size();

        SortedSet<Integer> ancestors =
                blockFirstColToAncestors.getOrDefault(firstColIndex, Collections.emptySortedSet());

        List<CellsWithRowIndex> res = new ArrayList<>();
        res.add(new CellsWithRowIndex(cells, curRowIndex));

        for (int row = curRowIndex + 1; row < rowSize; row++) {
            List<DCell> line = dTable.rows().get(row);

            if (isPkCellAllEmpty(line)) {  // 主键所在格子为空，还是本record
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
                if (thisCell.isCellEmpty()) { // 本格为空，内部的嵌套block，忽略
                } else { //本格不为空 -》 是这个block了
                    res.add(new CellsWithRowIndex(line.subList(firstColIndex, firstColIndex + colSize), row));
                }
            } else { // 下一个record，结束
                break;
            }
        }

        return res;
    }

    /**
     * 预计算每个 block 字段首列(cell-list index) -> 其词法包围的祖先 block 首列集合。
     * 列号空间与 parseBlock 的 firstColIndex 一致：都是 fieldIndices 过滤后的 cell list index，
     * 用 Span.fieldSpan 累加得到。模仿 Span.calcFieldSpanCheckLoop 的递归风格。
     */
    private static Map<Integer, SortedSet<Integer>> collectBlockAncestors(Structural structural) {
        Map<Integer, SortedSet<Integer>> result = new HashMap<>();
        collectStructuralBlockAncestors(structural, 0, Collections.emptySortedSet(), result);
        return result;
    }

    private static void collectStructuralBlockAncestors(Structural structural, int startCol,
                                                        SortedSet<Integer> outerAncestors,
                                                        Map<Integer, SortedSet<Integer>> result) {
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
                                                   Map<Integer, SortedSet<Integer>> result) {
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
                    result.put(startCol, outerAncestors);
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
                    result.put(startCol, outerAncestors);
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
                                                       Map<Integer, SortedSet<Integer>> result) {
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
                                                        Map<Integer, SortedSet<Integer>> result) {
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


    private int findColumnIndex(DCell cell) {
        int i = 0;
        for (DCell c : curRow) {
            if (c.col() == cell.col()) {
                return i;
            }
            i++;
        }
        return i;
    }

    public static List<Integer> getPkColumnIndices(TableSchema schema) {
        List<FieldSchema> pks = schema.primaryKey().fieldSchemas();
        List<Integer> pkIndices = new ArrayList<>(pks.size());
        for (FieldSchema pk : pks) {
            int idx = 0;
            for (FieldSchema f : schema.fields()) {
                int span = Span.fieldSpan(f);
                if (f == pk) {
                    for (int i = 0; i < span; i++) {
                        pkIndices.add(idx + i);
                    }
                    break;
                }
                idx += span;
            }
        }
        return pkIndices;
    }


}
