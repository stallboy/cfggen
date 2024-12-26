package configgen.i18n;

import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 每个表中的text字段，原始文本---映射到--->翻译文本。
 * 以原始文本为key，相同的原始文本，必然对应相同翻译文本
 */
class TextFinderByOrig implements TextFinder {
    private final SequencedMap<String, String> originalToTranslated = new LinkedHashMap<>();
    private final boolean isCrLfAsLf;

    public TextFinderByOrig(boolean isCrLfAsLf) {
        this.isCrLfAsLf = isCrLfAsLf;
    }

    @Override
    public String findText(String pk, List<String> fieldChain, String original) {
        String normalized = normalize(original, isCrLfAsLf);
        String text = originalToTranslated.get(normalized);
        if (text != null && !text.isEmpty()) {
            return text;
        }
        return null;
    }

    @Override
    public void foreachText(TextVisitor visitor) {
        for (Map.Entry<String, String> e : originalToTranslated.entrySet()) {
            visitor.visit(e.getKey(), e.getValue());
        }
    }


    static LangSwitch loadLangSwitch(Path path, String defaultLang, boolean isCrLfAsLf) {
        Map<String, LangTextFinder> lang2i18n = new TreeMap<>();
        try (Stream<Path> plist = Files.list(path)) {
            plist.forEach(langFilePath -> {
                String langName = langFilePath.getFileName().toString();
                if (langName.toLowerCase().endsWith(".csv")) {
                    langName = langName.substring(0, langName.length() - 4);
                    lang2i18n.put(langName, loadOneLang(langFilePath, isCrLfAsLf));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new LangSwitch(lang2i18n, defaultLang);
    }

    static LangTextFinder loadOneLang(Path path, boolean isCrLfAsLf) {
        List<CsvRow> rows = CSVUtil.read(path, "UTF-8");

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("国际化i18n文件为空");
        }
        CsvRow row0 = rows.getFirst();
        if (row0.getFieldCount() != 3) {
            throw new IllegalArgumentException("国际化i18n文件列数不为3");
        }

        LangTextFinder res = new LangTextFinder();
        for (CsvRow row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            if (row.getFieldCount() != 3) {
                System.out.println(row + " 不是3列，被忽略");
            } else {
                String table = row.getField(0);
                String original = row.getField(1);
                String translated = row.getField(2);
                original = normalize(original, isCrLfAsLf);

                TextFinderByOrig map = (TextFinderByOrig) res.getMap().computeIfAbsent(table, t -> new TextFinderByOrig(isCrLfAsLf));
                map.originalToTranslated.put(original, translated);
            }
        }
        return res;
    }


    private static final Pattern pattern = Pattern.compile("\r\n");

    private static String normalize(String text, boolean isCRLFAsLF) {
        if (isCRLFAsLF) {
            return pattern.matcher(text).replaceAll("\n");
        } else {
            return text;
        }
    }

}
