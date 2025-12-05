package configgen.geni18n;

import configgen.i18n.TextByIdFinder.OneText;
import configgen.i18n.I18nUtils;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public record TermFile(List<TermEntry> terms,
                       Map<String, List<OneText>> sources ) { // table -> list<original, translated>

    public record TermEntry(String original,
                            String translated,
                            String category,
                            String confidence,
                            String note) {
    }


    private static final String TERM_SHEET_NAME = "term";
    private static final String SOURCE_SHEET_NAME = "source";

    public static TermFile readTermFile(Path termFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(termFile.toFile(), new ReadingOptions(true, false))) {
            // 读取 term sheet
            Optional<Sheet> termSheet = wb.findSheet(TERM_SHEET_NAME);
            List<TermEntry> terms = new ArrayList<>();
            if (termSheet.isPresent()) {
                List<Row> rows = termSheet.get().read();
                for (Row row : rows) {
                    String original = row.getCellAsString(0).orElse("");
                    String translated = row.getCellAsString(1).orElse("");
                    String category = row.getCellAsString(2).orElse("");
                    String confidence = row.getCellAsString(3).orElse("");
                    String note = row.getCellAsString(4).orElse("");
                    // 规范化原始文本（与 TermChecker 保持一致）
                    String normalized = I18nUtils.normalize(original);
                    if (!original.isEmpty() && !translated.isEmpty()) {
                        terms.add(new TermEntry(normalized, translated, category, confidence, note));
                    }
                }
            }

            // 读取 source sheet
            Optional<Sheet> sourceSheet = wb.findSheet(SOURCE_SHEET_NAME);
            Map<String, List<OneText>> sources = new HashMap<>();
            if (sourceSheet.isPresent()) {
                List<Row> rows = sourceSheet.get().read();
                for (Row row : rows) {
                    String table = row.getCellAsString(0).orElse("");
                    String original = row.getCellAsString(1).orElse("");
                    String translated = row.getCellAsString(2).orElse("");
                    if (!table.isEmpty() && !original.isEmpty() && !translated.isEmpty()) {
                        String normalized = I18nUtils.normalize(original);
                        sources.computeIfAbsent(table, k -> new ArrayList<>())
                                .add(new OneText(normalized, translated));
                    }
                }
            }

            return new TermFile(terms, sources);

        } catch (IOException e) {
            throw new RuntimeException("读取术语文件失败: " + termFile, e);
        }
    }

    public static void writeTermFile(Path termFile, TermFile termFileData) {
        try (java.io.OutputStream os = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(termFile.toFile()))) {
            try (Workbook wb = new Workbook(os, "term", "1.0")) {
                // 写入 term sheet
                Worksheet termWs = wb.newWorksheet(TERM_SHEET_NAME);
                List<TermEntry> terms = termFileData.terms();
                for (int i = 0; i < terms.size(); i++) {
                    TermEntry term = terms.get(i);
                    termWs.inlineString(i, 0, term.original());
                    termWs.inlineString(i, 1, term.translated());
                    termWs.inlineString(i, 2, term.category());
                    termWs.inlineString(i, 3, term.confidence());
                    termWs.inlineString(i, 4, term.note());
                }

                // 写入 source sheet
                Worksheet sourceWs = wb.newWorksheet(SOURCE_SHEET_NAME);
                Map<String, List<OneText>> sources = termFileData.sources();
                int rowIndex = 0;
                for (Map.Entry<String, List<OneText>> entry : sources.entrySet()) {
                    String table = entry.getKey();
                    for (OneText source : entry.getValue()) {
                        sourceWs.inlineString(rowIndex, 0, table);
                        sourceWs.inlineString(rowIndex, 1, source.original());
                        sourceWs.inlineString(rowIndex, 2, source.translated());
                        rowIndex++;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<TermEntry> mergeTerms(List<TermEntry> existing, List<TermEntry> newTerms) {
        // 按 original 建立映射
        Map<String, TermEntry> map = new LinkedHashMap<>();
        for (TermEntry term : existing) {
            map.put(term.original(), term);
        }
        // 合并新术语
        for (TermEntry newTerm : newTerms) {
            TermEntry existingTerm = map.get(newTerm.original());
            if (existingTerm == null) {
                map.put(newTerm.original(), newTerm);
            } else {
                // 更新翻译、类别和备注（如果新术语提供了非空值）
                String translated = newTerm.translated().isEmpty() ? existingTerm.translated() : newTerm.translated();
                String category = newTerm.category().isEmpty() ? existingTerm.category() : newTerm.category();
                String confidence = newTerm.confidence().isEmpty() ? existingTerm.confidence() : newTerm.confidence();
                String note = newTerm.note().isEmpty() ? existingTerm.note() : newTerm.note();
                map.put(newTerm.original(), new TermEntry(newTerm.original(), translated, category, confidence, note));
            }
        }
        return new ArrayList<>(map.values());
    }


    public static List<OneText> loadTerm(Path termFilepath) {
        try (ReadableWorkbook wb = new ReadableWorkbook(termFilepath.toFile(), new ReadingOptions(true, false))) {
            Optional<Sheet> sheet = wb.findSheet(TERM_SHEET_NAME);
            if (sheet.isEmpty()) {
                return List.of();
            }
            List<Row> rows = sheet.get().read();
            List<OneText> result = new ArrayList<>(rows.size());
            for (Row row : rows) {
                String c0 = row.getCellAsString(0).orElse("");
                String c1 = row.getCellAsString(1).orElse("");
                String normalized = I18nUtils.normalize(c0);
                if (!c0.isEmpty() && !c1.isEmpty()) {
                    result.add(new OneText(normalized, c1));
                }
            }
            return result;

        } catch (IOException e) {
            throw new RuntimeException("read term file=%s error".formatted(termFilepath), e);
        }
    }

}
