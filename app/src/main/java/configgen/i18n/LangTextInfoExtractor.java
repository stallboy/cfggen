package configgen.i18n;

import configgen.schema.HasText;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import configgen.value.ValueUtil;

import java.util.*;

import static configgen.i18n.TextFinderById.*;


class LangTextInfoExtractor {

    public static LangTextInfo extract(CfgValue cfgValue) {
        Map<String, TextFinder> tableMap = new LinkedHashMap<>(16);
        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            if (HasText.hasText(vTable.schema())) {
                TextFinderById finder = new TextFinderById();
                finder.nullableDescriptionName = vTable.schema().meta().getStr("lang", null);
                for (Map.Entry<CfgValue.Value, CfgValue.VStruct> e : vTable.primaryKeyMap().entrySet()) {
                    CfgValue.Value pk = e.getKey();
                    String pkStr = pk.packStr();
                    CfgValue.VStruct vStruct = e.getValue();
                    String description = finder.nullableDescriptionName != null ?
                            ValueUtil.extractFieldValueStr(vStruct, finder.nullableDescriptionName) : null;

                    OneRecord record = new OneRecord(description, new ArrayList<>(finder.fieldChainToIndex.size()));
                    ForeachValue.foreachValue(new TextValueVisitor(finder, record), vStruct, pk, List.of());
                    if (!record.texts().isEmpty()) {
                        finder.pkToTexts.put(pkStr, record);
                    }
                }
                if (!finder.pkToTexts.isEmpty()) {
                    String tableName = vTable.name();
                    tableMap.put(tableName, finder);

                    long txtCount = finder.pkToTexts.values().stream().mapToLong(
                            r -> r.texts().stream().filter(Objects::nonNull).count()).sum();
                    Logger.verbose("%40s: %8d %8d", tableName, finder.pkToTexts.size(), txtCount);
                }
            }
        }
        return LangTextInfo.of(tableMap);
    }

    private static class TextValueVisitor extends ForeachValue.ValueVisitorForPrimitive {

        private final TextFinderById finder;
        private final OneRecord record;

        public TextValueVisitor(TextFinderById finder, OneRecord record) {
            this.finder = finder;
            this.record = record;
        }

        @Override
        public void visitPrimitive(CfgValue.PrimitiveValue primitiveValue, CfgValue.Value pk, List<String> fieldChain) {
            if (primitiveValue instanceof CfgValue.VText vText) {
                String original = vText.original().trim();
                String translated = vText.translated();
                if (original.isEmpty() && translated.isEmpty()) {
                    return;
                }

                // 存之前也normalize
                String normalized = Utils.normalize(original);
                OneText oneText = new OneText(normalized, translated);

                String fieldChainStr = fieldChainStr(fieldChain);
                int idx = finder.fieldChainToIndex.computeIfAbsent(fieldChainStr, k -> finder.fieldChainToIndex.size());
                // 保证中间塞上null
                while (record.texts().size() <= idx) {
                    record.texts().add(null);
                }
                record.texts().set(idx, oneText);
            }
        }
    }

}
