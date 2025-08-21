package configgen.i18n;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static configgen.i18n.TextFinderById.*;

/**
 * key：top module name
 */
class LangTextInfo extends LinkedHashMap<String, LangTextInfo.TopModuleTextInfo> {

    /**
     * key：table name
     */
    static class TopModuleTextInfo extends LinkedHashMap<String, TextFinderById> {

        public void save(OutputStream os, I18nStat stat) throws IOException {
            try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
                for (var e : entrySet()) {
                    String table = e.getKey();
                    TextFinderById tf = e.getValue();
                    Worksheet ws = wb.newWorksheet(table);
                    saveSheet(tf, ws, table, stat);
                }
            }
        }

        void saveSheet(TextFinderById finder, Worksheet ws, String table, I18nStat stat) {
            boolean hasDescriptionColumn = finder.nullableDescriptionName != null;
            int offset = hasDescriptionColumn ? 2 : 1;

            { // 写第一行，header
                ws.inlineString(0, 0, "id");
                if (hasDescriptionColumn) {
                    ws.inlineString(0, 1, finder.nullableDescriptionName);
                }
                int idx = 0;
                for (String field : finder.fieldChainToIndex.keySet()) {
                    int c = idx * 2 + offset;
                    ws.inlineString(0, c, field);
                    ws.inlineString(0, c + 1, "t(" + field + ")");
                    idx++;
                }
            }

            int r = 1;
            for (var e : finder.pkToTexts.entrySet()) {
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

    public static LangTextInfo of(Map<String, TextFinder> tableMap) {
        LangTextInfo res = new LangTextInfo();
        for (Map.Entry<String, TextFinder> e : tableMap.entrySet()) {
            String table = e.getKey();
            TextFinderById finder = (TextFinderById) e.getValue();
            TopModuleTextInfo file = res.computeIfAbsent(getTopModule(table),
                    k -> new TopModuleTextInfo());
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

    public void save(Path wroteDir, I18nStat stat) throws IOException {
        for (var e : entrySet()) {
            String topModuleFn = e.getKey() + ".xlsx";
            File dst = wroteDir.resolve(topModuleFn).toFile();
            TopModuleTextInfo topModule = e.getValue();
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(dst))) {
                topModule.save(os, stat);
            }
        }
    }


}
