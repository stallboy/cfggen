package configgen.schema;

import static configgen.schema.CfgSchemaErrs.*;

/**
 * 检测 block 字段首列是否与任意外层祖先 block 首列重合，重合则报 {@link BlockFirstColOverlap}。
 * <p>
 * 重合最典型的情形：外层 block 的元素 struct 的【首字段】本身又是 block —— 此时外层 block 首列（= 元素 struct 起始列）
 * 与内层 block 首列落在同一列。该列同时承担内外两层 block 的“项标识”，{@code VTableParser.parseBlock} 无法区分，
 * 会静默丢数据，故 schema 阶段直接拒绝。
 * <p>
 * 列号计算与递归范围由 {@link BlockAncestorWalker} 统一实现（只对 Block 格式的 list/map 字段展开元素），
 * 与解析时 {@code VTableBlockParser} 实际会触发首列重合的范围精确对应。
 */
public final class BlockFirstColOverlapChecker {
    private BlockFirstColOverlapChecker() {
    }

    public static void check(CfgSchema cfgSchema, CfgSchemaErrs errs) {
        for (TableSchema table : cfgSchema.tableMap().values()) {
            if (!table.isJson()) {
                BlockAncestorWalker.walk(table, (structural, field, startCol, ancestors) -> {
                    // 本 block 首列已是某个外层祖先 block 的首列 => 重合 => 该列无法同时标识内外两层
                    if (ancestors.contains(startCol)) {
                        errs.addErr(new BlockFirstColOverlap(structural.fullName(), field.name()));
                    }
                });
            }
        }
    }
}
