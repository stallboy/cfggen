package configgen.i18n;

import configgen.util.Logger;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static configgen.i18n.TextFinderById.*;

/**
 * key：top module name
 */
class LangTextInfo extends LinkedHashMap<String, LangTextInfo.TopModuleTextInfo> {

    public static String getTodoFileName(String lang) {
        return "_todo_" + lang + ".xlsx";
    }

    public static final String TODO_SHEET_NAME = "todo";

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

    public void saveTodo(OutputStream os) throws IOException {
        try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
            // Create todo sheet (not translated texts)
            Worksheet todoWs = wb.newWorksheet(TODO_SHEET_NAME);
            createTodo(todoWs, false);

            // Create done sheet (already translated texts)  
            Worksheet doneWs = wb.newWorksheet("done(参考用)");
            createTodo(doneWs, true);
        }
    }


    private void createTodo(Worksheet ws, boolean doneOnly) {
        // Write header
        ws.inlineString(0, 0, "table");
        ws.inlineString(0, 1, "id");
        ws.inlineString(0, 2, "fieldChain");
        ws.inlineString(0, 3, "original");
        ws.inlineString(0, 4, "translated");

        int row = 1;
        for (var e : entrySet()) {
            for (var t : e.getValue().entrySet()) {
                String table = t.getKey();
                TextFinderById finder = t.getValue();
                List<String> fieldChainList = finder.fieldChainToIndex.keySet().stream().toList();
                for (var r : finder.pkToTexts.entrySet()) {
                    String pk = r.getKey();
                    OneRecord record = r.getValue();
                    int idx = 0;
                    for (OneText ot : record.texts()) {
                        if (ot != null) {
                            boolean isTranslated = !ot.translated().isEmpty();
                            if ((doneOnly && isTranslated) || (!doneOnly && !isTranslated)) {
                                ws.inlineString(row, 0, table);
                                ws.inlineString(row, 1, pk);
                                ws.inlineString(row, 2, fieldChainList.get(idx));
                                ws.inlineString(row, 3, ot.original());
                                ws.inlineString(row, 4, ot.translated());
                                row++;
                            }
                        }
                        idx++;
                    }
                }
            }
        }
    }


    public static void mergeTodo(LangTextFinder langTextFinder, File todoFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile,
                new ReadingOptions(true, false))) {

            // 只处理todo分页，done分页的信息已经在其他文件中读过了
            Optional<Sheet> todoSheet = wb.findSheet(TODO_SHEET_NAME);

            if (todoSheet.isPresent()) {
                List<Row> rows = todoSheet.get().read();
                if (rows.size() > 1) {
                    for (int i = 1; i < rows.size(); i++) {
                        Row row = rows.get(i);
                        Optional<String> table = getCellAsString(row, 0);
                        Optional<String> pk = getCellAsString(row, 1);
                        Optional<String> fieldChain = getCellAsString(row, 2);
                        Optional<String> original = getCellAsString(row, 3);
                        Optional<String> translated = getCellAsString(row, 4);

                        // 当一个翻译项在item.xlsx和TODO文件都存在时，优先用TODO里的，在TODO的翻译为空时，会用item.xlsx里的
                        if (translated.isPresent() && !translated.get().isEmpty() &&
                                table.isPresent() && pk.isPresent() && fieldChain.isPresent() && original.isPresent()) {

                            TextFinderById finder = (TextFinderById) langTextFinder.getTableTextFinder(table.get());
                            if (finder != null) {
                                // 查找对应的记录并更新翻译
                                OneRecord record = finder.pkToTexts.get(pk.get());
                                if (record != null) {
                                    Integer fieldIndex = finder.fieldChainToIndex.get(fieldChain.get());
                                    if (fieldIndex != null && fieldIndex < record.texts().size()) {
                                        // 读进来的就做normalize
                                        String normalized = Utils.normalize(original.get());
                                        OneText oldText = record.texts().get(fieldIndex);

                                        if (oldText != null && oldText.original().equals(normalized)) {
                                            OneText newText = new OneText(oldText.original(), translated.get());
                                            record.texts().set(fieldIndex, newText);
                                        } else {
                                            Logger.log("Field chain %s with original %s not found in record with pk %s in table %s, skipping row %d",
                                                    fieldChain.get(), original.get(), pk.get(), table.get(), i + 1);
                                        }
                                    } else {
                                        Logger.log("Field chain %s not found in table %s, skipping row %d",
                                                fieldChain.get(), table.get(), i + 1);
                                    }
                                } else {
                                    Logger.log("Record with pk %s not found in table %s, skipping row %d",
                                            pk.get(), table.get(), i + 1);
                                }
                            } else {
                                Logger.log("Table %s not found in langTextFinder, skipping row %d",
                                        table.get(), i + 1);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + todoFile.getPath(), e);
        }
    }

}
