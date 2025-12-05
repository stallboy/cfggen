package configgen.geni18n;

import configgen.geni18n.TodoFile.Line;
import configgen.i18n.I18nUtils;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static configgen.i18n.I18nUtils.getCellAsString;

public record TodoEdit(List<TodoEntry> todo,
                       List<Line> done) {

    public static class TodoEntry {
        public final String table;
        public final String id;
        public final String fieldChain;
        public final String original;
        public final String translated;
        public String aiTranslated;
        public String confidence;
        public String note;

        public TodoEntry(String table, String id, String fieldChain, String original, String translated) {
            this.table = table;
            this.id = id;
            this.fieldChain = fieldChain;
            this.original = original;
            this.translated = translated;
        }

        public boolean hasTranslated() {
            return !translated.isBlank() || !aiTranslated.isBlank();
        }
    }

    public static class TodoOriginalsByTable extends LinkedHashMap<String, Set<String>> {

    }

    public static class DoneByTable extends LinkedHashMap<String, Map<String, String>> {

    }


    public DoneByTable parseDoneByTable() {
        DoneByTable doneByTable = new DoneByTable();
        if (done.size() < 2) {
            return doneByTable;
        }

        for (Line line : done.subList(1, done.size())) {
            String table = line.table();
            String original = line.original();
            String translated = line.translated();
            if (!original.isEmpty() && !translated.isEmpty()) {
                doneByTable.computeIfAbsent(table, k -> new HashMap<>())
                        .put(original, translated);
            }
        }
        return doneByTable;
    }

    /**
     * 使用done里的已翻译内容填到todo，返回仍需要翻译的
     * @param doneByTable done里的已翻译内容
     * @return Map<String, Set<String>> key是table，value是todoOriginals
     */
    public TodoOriginalsByTable useTranslationsInDoneIfSameOriginal(DoneByTable doneByTable) {
        TodoOriginalsByTable todoOriginalsByTable = new TodoOriginalsByTable();
        for (TodoEntry entry : todo) {
            if (entry.hasTranslated()) {
                continue;
            }
            Map<String, String> map = doneByTable.get(entry.table);
            if (map != null) {
                // 如果done sheet中已有相同original的翻译，则使用该翻译
                String translated = map.get(entry.original);
                if (translated != null) {
                    // 更新entry的translated字段
                    entry.aiTranslated = translated;
                    continue;
                }
            }
            // 按table分组添加到结果映射中
            todoOriginalsByTable.computeIfAbsent(entry.table, k -> new LinkedHashSet<>())
                    .add(entry.original);
        }
        return todoOriginalsByTable;
    }

    /**
     * AI返回的翻译结果
     */
    public record AITranslationEntry(@NotNull String translated,
                                     String confidence,
                                     String note) {
    }

    public static class AITranslationResult extends LinkedHashMap<String, AITranslationEntry> {

    }

    /**
     * 使用ai返回结果，填到todo里
     * @param aiResult ai返回的结果
     */
    public void useAITranslationResult(AITranslationResult aiResult) {
        for (TodoEntry entry : todo) {
            if (entry.hasTranslated()) {
                continue;
            }

            String normalized = I18nUtils.normalize(entry.original);
            AITranslationEntry t = aiResult.get(normalized);
            if (t != null) {
                entry.aiTranslated = t.translated;
                entry.confidence = t.confidence == null ? "" : t.confidence;
                entry.note = t.note == null ? "" : t.note;
            }
        }
    }

    public static TodoEdit read(Path todoFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(todoFile.toFile(),
                new ReadingOptions(true, false))) {

            Optional<Sheet> todoSheet = wb.findSheet(TodoFile.TODO_SHEET_NAME);
            Optional<Sheet> doneSheet = wb.findSheet(TodoFile.DONE_SHEET_NAME);
            List<TodoEntry> todo;
            if (todoSheet.isPresent()) {
                todo = readTodoSheet(todoSheet.get());
            } else {
                todo = List.of();
            }

            List<Line> done;
            if (doneSheet.isPresent()) {
                done = TodoFile.readSheetToLines(doneSheet.get());
            } else {
                done = List.of();
            }

            return new TodoEdit(todo, done);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + todoFile, e);
        }
    }

    private static List<TodoEntry> readTodoSheet(Sheet sheet) throws IOException {
        List<Row> rows = sheet.read();
        List<TodoEntry> entries = new ArrayList<>(rows.size());
        for (Row row : rows) {
            String table = getCellAsString(row, 0).orElse("");
            String id = getCellAsString(row, 1).orElse("");
            String fieldChain = getCellAsString(row, 2).orElse("");
            String original = getCellAsString(row, 3).orElse("");
            String translated = getCellAsString(row, 4).orElse("");
            String aiTranslated = getCellAsString(row, 5).orElse("");
            String confidence = getCellAsString(row, 6).orElse("");
            String note = getCellAsString(row, 7).orElse("");
            // 原则，original读进来就做normalize
            String normalized = I18nUtils.normalize(original);
            TodoEntry entry = new TodoEntry(table, id, fieldChain, normalized, translated);
            entry.aiTranslated = aiTranslated;
            entry.confidence = confidence;
            entry.note = note;
            entries.add(entry);
        }
        return entries;
    }


    public void save(Path todoFilePath) {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(todoFilePath.toFile()))) {
            save(os);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void save(OutputStream os) throws IOException {
        try (Workbook wb = new Workbook(os, "cfg", "1.0")) {
            // Create todo sheet (not translated texts)
            Worksheet todoWs = wb.newWorksheet(TodoFile.TODO_SHEET_NAME);
            saveTodoSheet(todoWs, todo);

            // Create done sheet (already translated texts)
            Worksheet doneWs = wb.newWorksheet(TodoFile.DONE_SHEET_NAME);
            TodoFile.saveLinesToSheet(doneWs, done);
        }
    }

    private static void saveTodoSheet(Worksheet ws, List<TodoEntry> entries) {
        int row = 0;
        for (TodoEntry entry : entries) {
            ws.inlineString(row, 0, entry.table);
            ws.inlineString(row, 1, entry.id);
            ws.inlineString(row, 2, entry.fieldChain);
            ws.inlineString(row, 3, entry.original);
            ws.inlineString(row, 4, entry.translated);
            ws.inlineString(row, 5, entry.aiTranslated);
            ws.inlineString(row, 6, entry.confidence);
            ws.inlineString(row, 7, entry.note);
            row++;
        }
    }


}
