package configgen.geni18n;

import configgen.i18n.I18nUtils;
import configgen.i18n.LangTextFinder;
import configgen.i18n.TextByIdFinder;
import configgen.schema.HasText;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.ForeachValue;
import configgen.value.ValueUtil;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static configgen.i18n.TextByIdFinder.*;

/**
 * key：top module name
 */
public class LangText extends LinkedHashMap<String, LangText.TopModuleText> {

    /**
     * key：table name
     */
    public static class TopModuleText extends LinkedHashMap<String, TextByIdFinder> {

        public void save(OutputStream os, LangStat stat) throws IOException {
            try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
                for (var e : entrySet()) {
                    String table = e.getKey();
                    TextByIdFinder tf = e.getValue();
                    Worksheet ws = wb.newWorksheet(table);
                    saveSheet(tf, ws, table, stat);
                }
            }
        }

        void saveSheet(TextByIdFinder finder, Worksheet ws, String table, LangStat stat) {
            boolean hasDescriptionColumn = finder.getNullableDescriptionName() != null;
            int offset = hasDescriptionColumn ? 2 : 1;

            { // 写第一行，header
                ws.inlineString(0, 0, "id");
                if (hasDescriptionColumn) {
                    ws.inlineString(0, 1, finder.getNullableDescriptionName());
                }
                int idx = 0;
                for (String field : finder.getFieldChainToIndex().keySet()) {
                    int c = idx * 2 + offset;
                    ws.inlineString(0, c, field);
                    ws.inlineString(0, c + 1, "t(" + field + ")");
                    idx++;
                }
            }

            int r = 1;
            for (var e : finder.getPkToTexts().entrySet()) {
                String pk = e.getKey();
                OneRecord record = e.getValue();

                ws.inlineString(r, 0, pk);
                if (hasDescriptionColumn) {
                    ws.inlineString(r, 1, record.description());
                }

                int idx = 0;
                for (OneText text : record.texts()) {
                    if (text != null) {
                        stat.addOneTranslate(table, text.original(), text.translated());
                        int c = idx * 2 + offset;

                        ws.inlineString(r, c, text.original());
                        ws.inlineString(r, c + 1, text.translated());

                        if (text.translated().isEmpty()) {
                            stat.incNotTranslate(table);
                            ws.style(r, c + 1).fillColor("FF8800").set();
                        }
                    }
                    idx++;
                }
                r++;
            }
        }
    }

    public void save(Path wroteDir, LangStat stat) throws IOException {
        for (var e : entrySet()) {
            String topModuleFn = e.getKey() + ".xlsx";
            File dst = wroteDir.resolve(topModuleFn).toFile();
            TopModuleText topModule = e.getValue();
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(dst))) {
                topModule.save(os, stat);
            }
        }
    }

    /**
     * 可以直接用equals，但这里为了调试方便，打印出不匹配的table
     */
    public boolean equalsWithLog(LangText other) {
        if (size() != other.size()) {
            return false;
        }
        for (var e : entrySet()) {
            String topModule = e.getKey();
            TopModuleText thisTop = e.getValue();
            TopModuleText otherTop = other.get(topModule);
            if (otherTop == null) {
                return false;
            }

            if (thisTop.size() != otherTop.size()) {
                return false;
            }

            for (var w : thisTop.entrySet()) {
                String table = w.getKey();
                TextByIdFinder wroteTable = w.getValue();
                TextByIdFinder extractedTable = otherTop.get(table);
                if (!wroteTable.equals(extractedTable)) {
                    Logger.log("%s NOT match", table);
                    return false;
                }
            }
        }
        return true;
    }

    public static LangText ofFinder(Map<String, LangTextFinder.TextFinder> tableMap) {
        LangText res = new LangText();
        for (Map.Entry<String, LangTextFinder.TextFinder> e : tableMap.entrySet()) {
            String table = e.getKey();
            TextByIdFinder finder = (TextByIdFinder) e.getValue();
            TopModuleText file = res.computeIfAbsent(getTopModule(table),
                    k -> new TopModuleText());
            file.put(table, finder);
        }
        return res;
    }

    private static String getTopModule(String table) {
        int idx = table.indexOf('.');
        if (idx != -1) {
            return table.substring(0, idx);
        }
        return "_top";
    }

    public static LangText extract(CfgValue cfgValue) {
        Map<String, LangTextFinder.TextFinder> tableMap = new LinkedHashMap<>(16);
        for (CfgValue.VTable vTable : cfgValue.sortedTables()) {
            if (HasText.hasText(vTable.schema())) {
                TextByIdFinder finder = new TextByIdFinder();
                finder.setNullableDescriptionName( vTable.schema().meta().getStr("lang", null));
                for (Map.Entry<CfgValue.Value, CfgValue.VStruct> e : vTable.primaryKeyMap().entrySet()) {
                    CfgValue.Value pk = e.getKey();
                    String pkStr = pk.packStr();
                    CfgValue.VStruct vStruct = e.getValue();
                    String description = finder.getNullableDescriptionName() != null ?
                            ValueUtil.extractFieldValueStr(vStruct, finder.getNullableDescriptionName()) : null;

                    OneRecord record = new OneRecord(description, new ArrayList<>(finder.getFieldChainToIndex().size()));
                    ForeachValue.foreachValue(new TextValueVisitor(finder, record), vStruct, pk, List.of());
                    if (!record.texts().isEmpty()) {
                        finder.getPkToTexts().put(pkStr, record);
                    }
                }
                if (!finder.getPkToTexts().isEmpty()) {
                    String tableName = vTable.name();
                    tableMap.put(tableName, finder);

                    long txtCount = finder.getPkToTexts().values().stream().mapToLong(
                            r -> r.texts().stream().filter(Objects::nonNull).count()).sum();
                    Logger.verbose("extract %20s: %8d pks %8d texts", tableName, finder.getPkToTexts().size(), txtCount);
                }
            }
        }
        return LangText.ofFinder(tableMap);
    }

    private static class TextValueVisitor extends ForeachValue.ValueVisitorForPrimitive {

        private final TextByIdFinder finder;
        private final OneRecord record;

        public TextValueVisitor(TextByIdFinder finder, OneRecord record) {
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
                String normalized = I18nUtils.normalize(original);
                OneText oneText = new OneText(normalized, translated);

                String fieldChainStr = I18nUtils.fieldChainStr(fieldChain);
                int idx = finder.getFieldChainToIndex().computeIfAbsent(fieldChainStr, k -> finder.getFieldChainToIndex().size());
                // 保证中间塞上null
                while (record.texts().size() <= idx) {
                    record.texts().add(null);
                }
                record.texts().set(idx, oneText);
            }
        }
    }

}
