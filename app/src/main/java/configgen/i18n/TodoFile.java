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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static configgen.i18n.TextByIdFinder.*;
import static configgen.i18n.TextByIdFinder.getCellAsString;

public record TodoFile(List<Line> todo,
                       List<Line> done) {

    /**
     * 用于存储todo文件中的一行数据
     */
    public record Line(String table,
                       String id,
                       String fieldChain,
                       String original,
                       String translated) {
    }

    private static final Line HEADER = new Line("table", "id", "fieldChain", "original", "translated");


    public static String getTodoFileName(String lang) {
        return "_todo_" + lang + ".xlsx";
    }

    private static final String TODO_SHEET_NAME = "todo";
    private static final String DONE_SHEET_NAME = "参考用";

    public static TodoFile ofLangText(LangText lang) {
        java.util.List<Line> todoLines = new ArrayList<>(32);
        java.util.List<Line> doneLines = new ArrayList<>(32);
        todoLines.add(HEADER);
        doneLines.add(HEADER);

        for (var e : lang.entrySet()) {
            for (var t : e.getValue().entrySet()) {
                String table = t.getKey();
                TextByIdFinder finder = t.getValue();
                java.util.List<String> fieldChainList = finder.fieldChainToIndex.keySet().stream().toList();
                for (var r : finder.pkToTexts.entrySet()) {
                    String pk = r.getKey();
                    OneRecord record = r.getValue();
                    int idx = 0;
                    for (OneText ot : record.texts()) {
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
        return new TodoFile(todoLines, doneLines);
    }


    public static TodoFile read(Path todoFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile.toFile(),
                new ReadingOptions(true, false))) {

            Optional<Sheet> todoSheet = wb.findSheet(TODO_SHEET_NAME);
            Optional<Sheet> doneSheet = wb.findSheet(DONE_SHEET_NAME);
            List<Line> todo;
            if (todoSheet.isPresent()) {
                todo = readSheet(todoSheet.get());
            } else {
                todo = List.of();
            }

            List<Line> done;
            if (doneSheet.isPresent()) {
                done = readSheet(doneSheet.get());
            } else {
                done = List.of();
            }

            return new TodoFile(todo, done);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + todoFile, e);
        }
    }

    private static List<Line> readSheet(Sheet sheet) throws IOException {
        List<Row> rows = sheet.read();
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

    }

    public void save(Path todoFilePath) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(todoFilePath.toFile()))) {
            save(os);
        }
    }

    private void save(OutputStream os) throws IOException {
        try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
            // Create todo sheet (not translated texts)
            Worksheet todoWs = wb.newWorksheet(TODO_SHEET_NAME);
            saveSheet(todoWs, todo);

            // Create done sheet (already translated texts)
            Worksheet doneWs = wb.newWorksheet(DONE_SHEET_NAME);
            saveSheet(doneWs, done);
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


    public static void readAndMergeToFinder(File todoFile, LangTextFinder langFinder) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile,
                new ReadingOptions(true, false))) {

            // 只处理todo分页，done分页的信息已经在其他文件中读过了
            Optional<Sheet> todoSheet = wb.findSheet(TODO_SHEET_NAME);
            if (todoSheet.isEmpty()) {
                return;
            }

            List<Line> lines = readSheet(todoSheet.get());
            for (int i = 1; i < lines.size(); i++) {
                Line line = lines.get(i);
                String table = line.table();
                String pk = line.id();
                String fieldChain = line.fieldChain();
                String original = line.original();
                String translated = line.translated();

                if (translated.isEmpty()) {
                    // 当一个翻译项在item.xlsx和TODO文件都存在时，优先用TODO里的，在TODO的翻译为空时，会用item.xlsx里的
                    continue;
                }
                TextByIdFinder finder = (TextByIdFinder) langFinder.getTextFinder(table);
                if (finder == null) {
                    Logger.log("Table %s not found in langTextFinder, skipping row %d",
                            table, i + 1);
                    continue;
                }

                OneRecord record = finder.pkToTexts.get(pk);
                if (record == null) {
                    Logger.log("Record with pk %s not found in table %s, skipping row %d",
                            pk, table, i + 1);
                    continue;
                }

                List<OneText> texts = record.texts();
                Integer fieldIndex = finder.fieldChainToIndex.get(fieldChain);
                if (fieldIndex == null || fieldIndex >= texts.size()) {
                    Logger.log("Field chain %s not found in table %s, skipping row %d",
                            fieldChain, table, i + 1);
                    continue;
                }

                OneText oldText = texts.get(fieldIndex);
                if (oldText == null || !oldText.original().equals(original)) {
                    Logger.log("Field chain %s with original %s not found in record with pk %s in table %s, skipping row %d",
                            fieldChain, original, pk, table, i + 1);
                    continue;
                }

                OneText newText = new OneText(original, translated);
                texts.set(fieldIndex, newText);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + todoFile.getPath(), e);
        }
    }


}
