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

    record OneRecord(String description,
                     List<OneText> texts) {
    }

    private String nullableDescriptionName;
    private final Map<String, Integer> fieldChainToIndex = new LinkedHashMap<>();
    private final Map<String, OneRecord> pkToTexts = new LinkedHashMap<>();

    @Override
    public String findText(String pk, List<String> fieldChain, String original) {
        String fieldChainStr = fieldChainStr(fieldChain);
        Integer idx = fieldChainToIndex.get(fieldChainStr);
        if (idx == null) {
            return null;
        }
        OneRecord line = pkToTexts.get(pk);
        if (line == null) {
            return null;
        }
        OneText txt = line.texts.get(idx);
        if (txt != null && txt.original.equals(original)) {
            return txt.translated;
        } else {
            return null;
        }
    }

    @Override
    public void foreachText(TextVisitor visitor) {
        for (OneRecord line : pkToTexts.values()) {
            for (OneText t : line.texts) {
                if (t != null) {
                    visitor.visit(t.original, t.translated);
                }
            }
        }
    }

    @Override
    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof TextFinderByPkAndFieldChain other)) {
            return false;
        }

        if (!Objects.equals(nullableDescriptionName, other.nullableDescriptionName)) {
            return false;
        }

        // 不用fieldChainToIndex.equals，是还要比较顺序一致
        if (fieldChainToIndex.size() != other.fieldChainToIndex.size()) {
            return false;
        }

        if (pkToTexts.size() != other.pkToTexts.size()) {
            return false;
        }

        {
            Iterator<Map.Entry<String, Integer>> f1 = fieldChainToIndex.entrySet().iterator();
            Iterator<Map.Entry<String, Integer>> f2 = other.fieldChainToIndex.entrySet().iterator();
            while (f1.hasNext()) {
                if (!f1.next().equals(f2.next())) {
                    return false;
                }
            }
        }

        {
            Iterator<Map.Entry<String, OneRecord>> r1 = pkToTexts.entrySet().iterator();
            Iterator<Map.Entry<String, OneRecord>> r2 = other.pkToTexts.entrySet().iterator();
            while (r1.hasNext()) {
                if (!r1.next().equals(r2.next())) {
                    return false;
                }
            }
        }

        return true;
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
                        langTextFinder.getMap().putAll(loadOneFile(filePath));
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return langTextFinder;
    }

    private static Map<String, TextFinderByPkAndFieldChain> loadOneFile(Path filePath) {
        Map<String, TextFinderByPkAndFieldChain> map = new LinkedHashMap<>();
        try (ReadableWorkbook wb = new ReadableWorkbook(filePath.toFile(),
                new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String tableName = sheet.getName().trim();
                List<Row> rawRows = sheet.read();
                if (rawRows.size() <= 1) {
                    continue;
                }

                TextFinderByPkAndFieldChain textFinder = new TextFinderByPkAndFieldChain();
                try {
                    loadOneSheet(rawRows, textFinder);
                } catch (Exception e) {
                    throw new RuntimeException("%s in %s read error".formatted(tableName,
                            filePath.toAbsolutePath().normalize().toString()), e);
                }
                map.put(tableName, textFinder);
            }
        } catch (IOException e) {
            throw new RuntimeException("read %s error".formatted(filePath.toAbsolutePath().normalize().toString()), e);
        }
        return map;
    }

    private static void loadOneSheet(List<Row> rawRows, TextFinderByPkAndFieldChain textFinder) {
        // 第一行是表头，分析
        Row header = rawRows.getFirst();
        int columnCount = header.getCellCount();
        if (columnCount <= 1) {
            return;
        }
        int[] tColumns = new int[columnCount]; // 翻译后的文本所在的列index
        int tColumnCnt = 0;
        for (int i = 2; i < columnCount; i++) { // 没有description列时，翻译文本列从2开始（0是pk，1是原始文本）
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

        boolean hasDescription = tColumns[0] > 2;
        if (hasDescription) {
            textFinder.nullableDescriptionName = header.getCellAsString(1).orElse("");
        }

        for (int r = 1, rowCount = rawRows.size(); r < rowCount; r++) {
            Row row = rawRows.get(r);
            Optional<String> pkCell = row.getCellAsString(0);
            if (pkCell.isEmpty()) {
                continue;
            }
            String pkStr = pkCell.get();

            String description = null;
            if (hasDescription) {
                description = row.getCellAsString(1).orElse("");
            }

            List<OneText> texts = new ArrayList<>(tColumnCnt);
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
                texts.add(ot);
            }

            textFinder.pkToTexts.put(pkStr, new OneRecord(description, texts));
        }
    }

}
