package configgen.i18n;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class TermFile {

    public static List<TextByIdFinder.OneText> loadTerm(Path termFilepath) {
        try (ReadableWorkbook wb = new ReadableWorkbook(termFilepath.toFile(), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                List<Row> rows = sheet.read();
                List<TextByIdFinder.OneText> result = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    String c0 = row.getCellAsString(0).orElse("");
                    String c1 = row.getCellAsString(1).orElse("");
                    String normalized = Utils.normalize(c0);
                    if (!c0.isEmpty() && !c1.isEmpty()) {
                        result.add(new TextByIdFinder.OneText(normalized, c1));
                    }
                }
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException("read term file=%s error".formatted(termFilepath), e);
        }
        return null;
    }


    // 内部数据记录类
    public record TermEntry(String original,
                            String translated,
                            String category,
                            String confidence,
                            String note) {
    }

    public static List<TermEntry> readTermFile(Path termFile) {
        try (ReadableWorkbook wb = new ReadableWorkbook(termFile.toFile(), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                List<Row> rows = sheet.read();
                List<TermEntry> result = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    String original = row.getCellAsString(0).orElse("");
                    String translated = row.getCellAsString(1).orElse("");
                    String category = row.getCellAsString(2).orElse("");
                    String confidence = row.getCellAsString(3).orElse("");
                    String note = row.getCellAsString(4).orElse("");
                    // 规范化原始文本（与 TermChecker 保持一致）
                    String normalized = Utils.normalize(original);
                    if (!original.isEmpty() && !translated.isEmpty()) {
                        result.add(new TermEntry(normalized, translated, category, confidence, note));
                    }
                }
                return result;
            }
        } catch (IOException e) {
            throw new RuntimeException("读取术语文件失败: " + termFile, e);
        }
        return Collections.emptyList();
    }

    public static void writeTermFile(Path termFile, List<TermEntry> terms) {
        try (java.io.OutputStream os = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(termFile.toFile()))) {
            try (Workbook wb = new Workbook(os, "term", "1.0")) {
                Worksheet ws = wb.newWorksheet("Sheet1");
                for (int i = 0; i < terms.size(); i++) {
                    TermEntry term = terms.get(i);
                    ws.inlineString(i, 0, term.original());
                    ws.inlineString(i, 1, term.translated());
                    ws.inlineString(i, 2, term.category());
                    ws.inlineString(i, 3, term.confidence());
                    ws.inlineString(i, 4, term.note());
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
}
