package configgen.i18n;

import org.dhatim.fastexcel.reader.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * 每个表中的text字段，pk+fieldChain作为id---映射到--->翻译文本。
 * 这是最完备的机制，可以相同的原始本文，不同的翻译文本。
 */
class TextFinderByPkAndFieldChain implements TextFinder {
    record OneText(String original,
                   String translated) {
    }

    private final Map<String, Integer> fieldChainToIndex = new HashMap<>();
    private final Map<String, OneText[]> pkToTexts = new HashMap<>();

    @Override
    public String findText(String pk, List<String> fieldChain, String original) {
        String fieldChainStr = fieldChainStr(fieldChain);
        Integer idx = fieldChainToIndex.get(fieldChainStr);
        if (idx == null) {
            return null;
        }
        OneText[] line = pkToTexts.get(pk);
        if (line == null) {
            return null;
        }
        OneText txt = line[idx];
        if (txt != null && txt.original.equals(original)) {
            return txt.translated;
        } else {
            return null;
        }
    }

    @Override
    public void foreachText(TextVisitor visitor) {
        for (OneText[] line : pkToTexts.values()) {
            for (OneText t : line) {
                if (t != null) {
                    visitor.visit(t.original, t.translated);
                }
            }
        }
    }

    static String fieldChainStr(List<String> fieldChain) {
        return fieldChain.size() == 1 ? fieldChain.getFirst() : String.join("-", fieldChain);
    }

    static LangSwitch loadLangSwitch(Path path, String defaultLang) {
        Map<String, LangTextFinder> lang2i18n = new TreeMap<>();
        try (Stream<Path> plist = Files.list(path)) {
            plist.forEach(langDir -> {
                if (Files.isDirectory(langDir)) {
                    String langName = langDir.getFileName().toString();
                    lang2i18n.put(langName, loadOneLang(langDir));
                }

            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new LangSwitch(lang2i18n, defaultLang);
    }

    static LangTextFinder loadOneLang(Path langDir) {
        LangTextFinder langTextFinder = new LangTextFinder();
        try (Stream<Path> plist = Files.list(langDir)) {
            plist.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    if (filePath.getFileName().toString().toLowerCase().endsWith(".xlsx")) {
                        loadOneFile(filePath, langTextFinder);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return langTextFinder;
    }

    private static void loadOneFile(Path filePath, LangTextFinder langTextFinder) {
        try (ReadableWorkbook wb = new ReadableWorkbook(filePath.toFile(), new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String tableName = sheet.getName().trim();
                List<Row> rawRows = sheet.read();
                if (rawRows.size() <= 1) {
                    continue;
                }

                TextFinderByPkAndFieldChain textFinder = new TextFinderByPkAndFieldChain();
                langTextFinder.getMap().put(tableName, textFinder);
                loadOneSheet(rawRows, textFinder);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadOneSheet(List<Row> rawRows, TextFinderByPkAndFieldChain textFinder) {
        Row header = rawRows.getFirst();
        int columnCount = header.getCellCount();
        if (columnCount <= 1) {
            return;
        }
        int[] tColumns = new int[columnCount];
        int tColumnCnt = 0;
        for (int i = 1; i < columnCount; i++) {
            Optional<String> cell = header.getCellAsString(i);
            if (cell.isPresent()) {
                String field = cell.get();
                if (field.startsWith("t(") && field.endsWith(")")) {
                    String fieldChainStr = field.substring(2, field.length() - 1);
                    textFinder.fieldChainToIndex.put(fieldChainStr, tColumnCnt);
                    tColumns[tColumnCnt] = i;
                    tColumnCnt++;
                }
            }
        }

        if (tColumnCnt == 0) {
            return;
        }

        for (int r = 1, rowCount = rawRows.size(); r < rowCount; r++) {
            Row row = rawRows.get(r);
            Optional<String> pkCell = row.getCellAsString(0);
            if (pkCell.isEmpty()) {
                continue;
            }
            String pkStr = pkCell.get();

            OneText[] texts = new OneText[tColumnCnt];
            for (int i = 0; i < tColumnCnt; i++) {
                int translateCol = tColumns[i];
                int originalCol = translateCol - 1;
                Optional<String> oC = row.getCellAsString(originalCol);
                Optional<String> tC = row.getCellAsString(translateCol);

                OneText ot;
                if (oC.isEmpty() && tC.isEmpty()) {
                    ot = null;
                } else {
                    String original = oC.orElse("");
                    String translate = tC.orElse("");
                    ot = new OneText(original, translate);
                }
                texts[i] = ot;
            }

            textFinder.pkToTexts.put(pkStr, texts);
        }
    }

}
