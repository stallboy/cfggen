package configgen.schema;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import static configgen.schema.FieldFormat.AutoOrPack.PACK;

/**
 * 「block 祖先」遍历骨架：在 fieldIndices 过滤后的 cell-list index 列号空间（用 {@link Span#fieldSpan} 累加）
 * 递归展开结构，每遇到一个 Block 格式的 list/map 字段就回调
 * {@link BlockFieldVisitor#onBlockField}，并携带「词法包围本 block 的所有外层祖先 block 首列集合」。
 * <p>
 * 列号计算与递归范围（pack/sep 不展开、只对 Block 格式的 list/map 字段展开元素、interface 各 impl 从
 * startCol+1 起）原本在 {@link BlockFirstColOverlapChecker} 与 {@code value.VTableBlockParser} 中各有一份
 * 逐行平行的拷贝，此处统一为唯一实现；两个调用方只在回调里保留各自差异（校验首列重合 / 收集祖先信息）。
 */
public final class BlockAncestorWalker {
    private BlockAncestorWalker() {
    }

    public interface BlockFieldVisitor {
        /**
         * @param structural      直接包含该 block 字段的结构（interface 场景为 impl struct）
         * @param field           Block 格式的 list/map 字段
         * @param startCol        该字段首列（cell-list index 列号空间）
         * @param outerAncestors  词法包围本 block 的所有外层祖先 block 首列集合（不含本列）
         */
        void onBlockField(Structural structural, FieldSchema field, int startCol,
                          SortedSet<Integer> outerAncestors);
    }

    public static void walk(Structural root, BlockFieldVisitor visitor) {
        walkStructural(root, 0, Collections.emptySortedSet(), visitor);
    }

    private static void walkStructural(Structural structural, int startCol,
                                       SortedSet<Integer> ancestors, BlockFieldVisitor visitor) {
        // pack/sep 结构只占 1 列，内部不可能展开 block（Span 计算时已保证占 1 列）
        if (structural.fmt() == PACK || structural.fmt() instanceof FieldFormat.Sep) {
            return;
        }
        int col = startCol;
        for (FieldSchema field : structural.fields()) {
            walkField(structural, field, col, ancestors, visitor);
            col += Span.fieldSpan(field);
        }
    }

    private static void walkField(Structural structural, FieldSchema field, int startCol,
                                  SortedSet<Integer> ancestors, BlockFieldVisitor visitor) {
        // pack/sep 字段只占 1 列，内部结构不展开（Span 也不为它们计算内部 span），不可能含 block。
        // 这也是断开 struct/interface 自引用环的关键：合法 schema 中自引用必须经 pack 截断
        // （否则 Span 计算会报 StructNestLoop），在 pack 处 return 即终止递归。
        if (field.fmt() == PACK || field.fmt() instanceof FieldFormat.Sep) {
            return;
        }
        switch (field.type()) {
            case FieldType.Primitive ignored -> {
            }
            case FieldType.StructRef sr -> walkFieldable(sr.obj(), startCol, ancestors, visitor);
            case FieldType.FList fl -> {
                if (field.fmt() instanceof FieldFormat.Block(int fix)) {
                    visitor.onBlockField(structural, field, startCol, ancestors);
                    // 元素内 block 的祖先 = 外层祖先 + 本 block 首列；
                    // fix>1 时横排的每个元素副本互为兄弟（互不为祖先），各自独立传入同一 itemAncestors
                    SortedSet<Integer> itemAncestors = concatSorted(ancestors, startCol);
                    int itemSpan = Span.simpleTypeSpan(fl.item());
                    for (int i = 0; i < fix; i++) {
                        walkSimpleType(fl.item(), startCol + i * itemSpan, itemAncestors, visitor);
                    }
                }
            }
            case FieldType.FMap fm -> {
                if (field.fmt() instanceof FieldFormat.Block(int fix)) {
                    visitor.onBlockField(structural, field, startCol, ancestors);
                    SortedSet<Integer> entryAncestors = concatSorted(ancestors, startCol);
                    int keySpan = Span.simpleTypeSpan(fm.key());
                    int entrySpan = keySpan + Span.simpleTypeSpan(fm.value());
                    // key 和 value 同属一个 entry，互为兄弟（互不为祖先）
                    for (int i = 0; i < fix; i++) {
                        int entryCol = startCol + i * entrySpan;
                        walkSimpleType(fm.key(), entryCol, entryAncestors, visitor);
                        walkSimpleType(fm.value(), entryCol + keySpan, entryAncestors, visitor);
                    }
                }
            }
        }
    }

    private static void walkFieldable(Fieldable fieldable, int startCol,
                                      SortedSet<Integer> ancestors, BlockFieldVisitor visitor) {
        switch (fieldable) {
            case Structural s -> walkStructural(s, startCol, ancestors, visitor);
            case InterfaceSchema is -> {
                // interface 第一列是 impl 名（在 startCol），各 impl 字段从 startCol+1 起（Span 用 max+1）
                for (StructSchema impl : is.impls()) {
                    walkStructural(impl, startCol + 1, ancestors, visitor);
                }
            }
        }
    }

    private static void walkSimpleType(FieldType.SimpleType st, int startCol,
                                       SortedSet<Integer> ancestors, BlockFieldVisitor visitor) {
        switch (st) {
            case FieldType.Primitive ignored -> {
            }
            case FieldType.StructRef sr -> walkFieldable(sr.obj(), startCol, ancestors, visitor);
        }
    }

    private static SortedSet<Integer> concatSorted(SortedSet<Integer> base, int add) {
        TreeSet<Integer> r = new TreeSet<>(base);
        r.add(add);
        return Collections.unmodifiableSortedSet(r);
    }
}
