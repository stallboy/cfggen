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
class TextFinderById implements TextFinder {
    record OneText(String original,
                   String translated) {
        public OneText {
            if (original == null || translated == null) {
                throw new IllegalArgumentException("original和translated都不能为null");
            }
        }
    }

    /**
     * @param description 可以为null
     * @param texts 里面元素可以为null
     */
    record OneRecord(String description,
                     List<OneText> texts) {

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof OneRecord(String otherDescription, List<OneText> otherTexts))) {
                return false;
            }
            if (!Objects.equals(description, otherDescription)) {
                return false;
            }
            // 可能一个里面有多余的null
            // <a,null,b>要跟<a,null,b,null>相等
            if (texts.size() == otherTexts.size()) {
                return texts.equals(otherTexts);
            }

            List<OneText> larger;
            List<OneText> smaller;
            if (texts.size() < otherTexts.size()) {
                smaller = texts;
                larger = otherTexts;
            } else {
                smaller = otherTexts;
                larger = texts;
            }

            if (!larger.subList(0, smaller.size()).equals(smaller)) {
                return false;
            }

            for (int i = smaller.size(); i < larger.size(); i++) {
                if (larger.get(i) != null) {
                    return false; // larger有多余的非null
                }
            }
            return true;
        }
    }

    String nullableDescriptionName;
    /**
     * 这个map的key是fieldChain的字符串表示
     * value是oneRecord.texts的index
     */
    final Map<String, Integer> fieldChainToIndex = new LinkedHashMap<>();
    final Map<String, OneRecord> pkToTexts = new LinkedHashMap<>();

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

        if (idx >= line.texts.size()) {
            return null;
        }

        OneText txt = line.texts.get(idx);
        String normalized = Utils.normalize(original);
        if (txt != null && txt.original.equals(normalized)) {
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
        if (!(otherObj instanceof TextFinderById other)) {
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

    public static LangSwitchable loadLangSwitch(Path path, String defaultLang) {
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

        return new LangSwitchable(lang2i18n, defaultLang);
    }

    public static LangTextFinder loadOneLang(Path langDir) {
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

    private static Map<String, TextFinderById> loadOneFile(Path filePath) {
        Map<String, TextFinderById> map = new LinkedHashMap<>();
        try (ReadableWorkbook wb = new ReadableWorkbook(filePath.toFile(),
                new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String tableName = sheet.getName().trim();
                List<Row> rawRows = sheet.read();
                if (rawRows.size() <= 1) {
                    continue;
                }

                TextFinderById textFinder = new TextFinderById();
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

    private static void loadOneSheet(List<Row> rawRows, TextFinderById textFinder) {
        // 第一行是表头，分析
        Row header = rawRows.getFirst();
        int columnCount = header.getCellCount();
        if (columnCount <= 1) {
            return;
        }
        int[] tColumns = new int[columnCount]; // 翻译后的文本所在的列index
        int tColumnCnt = 0;
        for (int i = 2; i < columnCount; i++) { // 没有description列时，翻译文本列从2开始（0是pk，1是原始文本）
            Optional<String> cell = getCellAsString(header, i);
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
            textFinder.nullableDescriptionName = getCellAsString(header, 1).orElse("");
        }

        for (int r = 1, rowCount = rawRows.size(); r < rowCount; r++) {
            Row row = rawRows.get(r);
            Optional<String> pkCell = getCellAsString(row, 0);
            if (pkCell.isEmpty()) {
                continue;
            }
            String pkStr = pkCell.get();

            String description = null;
            if (hasDescription) {
                description = getCellAsString(row, 1).orElse("");
            }

            List<OneText> texts = new ArrayList<>(tColumnCnt);
            for (int i = 0; i < tColumnCnt; i++) {
                int translateCol = tColumns[i];
                int originalCol = translateCol - 1;
                Optional<String> oC = getCellAsString(row, originalCol);
                Optional<String> tC = getCellAsString(row, translateCol);

                OneText ot;
                if (oC.isEmpty() && tC.isEmpty()) {
                    ot = null;
                } else {
                    String original = oC.orElse("");
                    String translate = tC.orElse("");
                    String normalized = Utils.normalize(original);
                    ot = new OneText(normalized, translate);
                }
                texts.add(ot);
            }

            textFinder.pkToTexts.put(pkStr, new OneRecord(description, texts));
        }
    }

    /**
     * 让number类型也返回string，因为翻译返回的excel有些格子是数字默认用了number
     */
    static private Optional<String> getCellAsString(Row row, int c) {
        Optional<Cell> cell = row.getOptionalCell(c);
        if (cell.isPresent()) {
            switch (cell.get().getType()){
                case NUMBER -> {
                    return Optional.of(cell.get().asNumber().toPlainString());
                }
                case STRING -> {
                    return Optional.of(cell.get().asString());
                }
                case EMPTY -> {
                    return Optional.of("");
                }
                default -> {
                    throw new IllegalArgumentException("不支持的单元格类型: " + cell.get().getType());
                }
            }
        } else {
            return Optional.empty();
        }
    }
}
