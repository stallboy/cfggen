package configgen.schema;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import static configgen.schema.CfgSchemaErrs.*;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldFormat.Sep;

/**
 * 检测 block 字段首列是否与任意外层祖先 block 首列重合，重合则报 {@link BlockFirstColOverlap}。
 * <p>
 * 重合最典型的情形：外层 block 的元素 struct 的【首字段】本身又是 block —— 此时外层 block 首列（= 元素 struct 起始列）
 * 与内层 block 首列落在同一列。该列同时承担内外两层 block 的“项标识”，{@code VTableParser.parseBlock} 无法区分，
 * 会静默丢数据，故 schema 阶段直接拒绝。
 * <p>
 * 列号计算与递归范围与 {@code VTableParser.collectBlockAncestors} 保持一致（只对 Block 格式的 list/map 字段展开元素），
 * 这样报错范围与解析时实际会触发首列重合的范围精确对应。
 */
public final class BlockFirstColOverlapChecker {
    private BlockFirstColOverlapChecker() {
    }

    public static void check(CfgSchema cfgSchema, CfgSchemaErrs errs) {
        for (TableSchema table : cfgSchema.tableMap().values()) {
            if (!table.isJson()) {
                checkStructural(table, 0, Collections.emptySortedSet(), errs);
            }
        }
    }

    private static void checkStructural(Structural structural, int startCol,
                                        SortedSet<Integer> ancestorBlockFirstCols, CfgSchemaErrs errs) {
        // pack/sep 结构只占 1 列，内部不可能展开 block（Span 计算时已保证占 1 列）
        if (structural.fmt() == PACK || structural.fmt() instanceof Sep) {
            return;
        }
        int col = startCol;
        for (FieldSchema field : structural.fields()) {
            checkField(field, col, ancestorBlockFirstCols, structural.fullName(), errs);
            col += Span.fieldSpan(field);
        }
    }

    private static void checkField(FieldSchema field, int startCol,
                                   SortedSet<Integer> ancestors, String structuralFullName,
                                   CfgSchemaErrs errs) {
        // pack/sep 字段只占 1 列，内部结构不展开，不可能含 block；也是断开 struct/interface 自引用环的关键
        if (field.fmt() == PACK || field.fmt() instanceof Sep) {
            return;
        }
        switch (field.type()) {
            case FieldType.Primitive ignored -> { }
            case FieldType.StructRef sr -> checkFieldable(sr.obj(), startCol, ancestors, errs);
            case FieldType.FList fl -> {
                if (field.fmt() instanceof FieldFormat.Block(int fix)) {
                    // 本 block 首列已是某个外层祖先 block 的首列 => 重合 => 该列无法同时标识内外两层
                    if (ancestors.contains(startCol)) {
                        errs.addErr(new BlockFirstColOverlap(structuralFullName, field.name()));
                    }
                    SortedSet<Integer> itemAncestors = concatSorted(ancestors, startCol);
                    int itemSpan = Span.simpleTypeSpan(fl.item());
                    for (int i = 0; i < fix; i++) {
                        checkSimpleType(fl.item(), startCol + i * itemSpan, itemAncestors, errs);
                    }
                }
            }
            case FieldType.FMap fm -> {
                if (field.fmt() instanceof FieldFormat.Block(int fix)) {
                    if (ancestors.contains(startCol)) {
                        errs.addErr(new BlockFirstColOverlap(structuralFullName, field.name()));
                    }
                    SortedSet<Integer> entryAncestors = concatSorted(ancestors, startCol);
                    int keySpan = Span.simpleTypeSpan(fm.key());
                    int entrySpan = keySpan + Span.simpleTypeSpan(fm.value());
                    for (int i = 0; i < fix; i++) {
                        int entryCol = startCol + i * entrySpan;
                        checkSimpleType(fm.key(), entryCol, entryAncestors, errs);
                        checkSimpleType(fm.value(), entryCol + keySpan, entryAncestors, errs);
                    }
                }
            }
        }
    }

    private static void checkFieldable(Fieldable fieldable, int startCol,
                                       SortedSet<Integer> ancestors, CfgSchemaErrs errs) {
        switch (fieldable) {
            case Structural s -> checkStructural(s, startCol, ancestors, errs);
            case InterfaceSchema is -> {
                // interface 第一列是 impl 名（在 startCol），各 impl 字段从 startCol+1 起（Span 用 max+1）
                for (StructSchema impl : is.impls()) {
                    checkStructural(impl, startCol + 1, ancestors, errs);
                }
            }
        }
    }

    private static void checkSimpleType(FieldType.SimpleType st, int startCol,
                                        SortedSet<Integer> ancestors, CfgSchemaErrs errs) {
        switch (st) {
            case FieldType.Primitive ignored -> { }
            case FieldType.StructRef sr -> checkFieldable(sr.obj(), startCol, ancestors, errs);
        }
    }

    private static SortedSet<Integer> concatSorted(SortedSet<Integer> base, int add) {
        TreeSet<Integer> r = new TreeSet<>(base);
        r.add(add);
        return Collections.unmodifiableSortedSet(r);
    }
}
