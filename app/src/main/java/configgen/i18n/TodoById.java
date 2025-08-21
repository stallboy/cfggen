package configgen.i18n;

import configgen.util.Logger;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static configgen.i18n.TextFinderById.getCellAsString;

class TodoById {

    public static String getTodoFileName(String lang) {
        return "_todo_" + lang + ".xlsx";
    }

    public static final String TODO_SHEET_NAME = "todo";
    public static final String DONE_SHEET_NAME = "done(参考用)";

    public static void saveTodo(OutputStream os, TodoFileInfo todoFileInfo) throws IOException {
        try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
            // Create todo sheet (not translated texts)
            Worksheet todoWs = wb.newWorksheet(TODO_SHEET_NAME);
            saveSheet(todoWs, todoFileInfo.todo);

            // Create done sheet (already translated texts)
            Worksheet doneWs = wb.newWorksheet(DONE_SHEET_NAME);
            saveSheet(doneWs, todoFileInfo.done);
        }
    }

    private static void saveSheet(Worksheet ws, List<Line> lines) {
        int row = 0;
        for (Line line : lines) {
            ws.inlineString(row, 0, line.table());
            ws.inlineString(row, 1, line.id());
            ws.inlineString(row, 2, line.fieldChain());
            ws.inlineString(row, 3, line.original());
            ws.inlineString(row, 4, line.translated());
            row++;
        }
    }


    public static void mergeTodo(LangTextFinder langTextFinder, File todoFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile,
                new ReadingOptions(true, false))) {

            // 只处理todo分页，done分页的信息已经在其他文件中读过了
            Optional<Sheet> todoSheet = wb.findSheet(TODO_SHEET_NAME);
            List<Line> lines = TodoFileInfo.readOneSheet(todoSheet);
            for (int i = 1; i < lines.size(); i++) {
                Line line = lines.get(i);
                String table = line.table();
                String pk = line.id();
                String fieldChain = line.fieldChain();
                String original = line.original();
                String translated = line.translated();

                // 当一个翻译项在item.xlsx和TODO文件都存在时，优先用TODO里的，在TODO的翻译为空时，会用item.xlsx里的
                if (!translated.isEmpty()) {
                    TextFinderById finder = (TextFinderById) langTextFinder.getTextFinder(table);
                    if (finder != null) {
                        // 查找对应的记录并更新翻译
                        TextFinderById.OneRecord record = finder.pkToTexts.get(pk);
                        if (record != null) {
                            Integer fieldIndex = finder.fieldChainToIndex.get(fieldChain);
                            if (fieldIndex != null && fieldIndex < record.texts().size()) {
                                TextFinderById.OneText oldText = record.texts().get(fieldIndex);

                                if (oldText != null && oldText.original().equals(original)) {
                                    TextFinderById.OneText newText = new TextFinderById.OneText(original, translated);
                                    record.texts().set(fieldIndex, newText);
                                } else {
                                    Logger.log("Field chain %s with original %s not found in record with pk %s in table %s, skipping row %d",
                                            fieldChain, original, pk, table, i + 1);
                                }
                            } else {
                                Logger.log("Field chain %s not found in table %s, skipping row %d",
                                        fieldChain, table, i + 1);
                            }
                        } else {
                            Logger.log("Record with pk %s not found in table %s, skipping row %d",
                                    pk, table, i + 1);
                        }
                    } else {
                        Logger.log("Table %s not found in langTextFinder, skipping row %d",
                                table, i + 1);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + todoFile.getPath(), e);
        }
    }

    /**
     * 用于存储todo文件中的一行数据
     */
    public record Line(String table, String id, String fieldChain, String original, String translated) {
    }

    private static final Line HEADER = new Line("table", "id", "fieldChain", "original", "translated");

    public record TodoFileInfo(List<Line> todo, List<Line> done) {
        static TodoFileInfo of(LangTextInfo lang) {
            List<Line> todoLines = new ArrayList<>(32);
            List<Line> doneLines = new ArrayList<>(32);
            todoLines.add(HEADER);
            doneLines.add(HEADER);

            for (var e : lang.entrySet()) {
                for (var t : e.getValue().entrySet()) {
                    String table = t.getKey();
                    TextFinderById finder = t.getValue();
                    List<String> fieldChainList = finder.fieldChainToIndex.keySet().stream().toList();
                    for (var r : finder.pkToTexts.entrySet()) {
                        String pk = r.getKey();
                        TextFinderById.OneRecord record = r.getValue();
                        int idx = 0;
                        for (TextFinderById.OneText ot : record.texts()) {
                            if (ot != null) {
                                Line line = new Line(table, pk, fieldChainList.get(idx), ot.original(), ot.translated());
                                if (ot.translated().isEmpty()) {
                                    todoLines.add(line);
                                } else {
                                    doneLines.add(line);
                                }
                            }
                            idx++;
                        }
                    }
                }
            }
            return new TodoFileInfo(todoLines, doneLines);
        }


        static TodoFileInfo read(File todoFile) {
            try (ReadableWorkbook wb = new ReadableWorkbook(todoFile,
                    new ReadingOptions(true, false))) {

                Optional<Sheet> todoSheet = wb.findSheet(TODO_SHEET_NAME);
                Optional<Sheet> doneSheet = wb.findSheet(DONE_SHEET_NAME);

                return new TodoFileInfo(readOneSheet(todoSheet),
                        readOneSheet(doneSheet));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read " + todoFile.getPath(), e);
            }
        }

        static List<Line> readOneSheet(Optional<Sheet> sheet) throws IOException {
            if (sheet.isPresent()) {
                List<Row> rows = sheet.get().read();
                List<Line> lines = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    String table = getCellAsString(row, 0).orElse("");
                    String id = getCellAsString(row, 1).orElse("");
                    String fieldChain = getCellAsString(row, 2).orElse("");
                    String original = getCellAsString(row, 3).orElse("");
                    String translated = getCellAsString(row, 4).orElse("");
                    // 原则，original读进来就做normalize
                    String normalized = Utils.normalize(original);
                    lines.add(new Line(table, id, fieldChain, normalized, translated));
                }
                return lines;
            } else {
                return List.of();
            }
        }


    }


}
