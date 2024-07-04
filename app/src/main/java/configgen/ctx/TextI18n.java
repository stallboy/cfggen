package configgen.ctx;

import configgen.util.CSVUtil;
import de.siegmar.fastcsv.reader.CsvRow;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public record TextI18n(Map<String, TableI18n> tableI18nMap) {
    public TableI18n getTableI18n(String table) {
        return tableI18nMap.get(table);
    }

    public record TableI18n(SequencedMap<String, String> original2text,
                            boolean isCRLFAsLF) {
        public String findText(String original) {
            String normalized = normalize(original, isCRLFAsLF);
            String text = original2text.get(normalized);
            if (text != null && !text.isEmpty()) {
                return text;
            }
            return null;
        }

        // 单个table，每行翻译保持跟文件里顺序一致
        public static TableI18n of(boolean isCRLFAsLF) {
            return new TableI18n(new LinkedHashMap<>(), isCRLFAsLF);
        }
    }

    // 语言根据字符串排序
    public static TextI18n of() {
        return new TextI18n(new TreeMap<>());
    }


    public static TextI18n loadFromCsvFile(Path path, boolean crlfaslf) {
        List<CsvRow> rows = CSVUtil.read(path, "UTF-8");

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("国际化i18n文件为空");
        }
        CsvRow row0 = rows.getFirst();
        if (row0.getFieldCount() != 3) {
            throw new IllegalArgumentException("国际化i18n文件列数不为3");
        }

        TextI18n textI18n = TextI18n.of();
        for (CsvRow row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            if (row.getFieldCount() != 3) {
                System.out.println(row + " 不是3列，被忽略");
            } else {
                String table = row.getField(0);
                String original = row.getField(1);
                String lang = row.getField(2);
                original = TextI18n.normalize(original, crlfaslf);

                TableI18n tableI18n = textI18n.tableI18nMap().computeIfAbsent(table, t -> TableI18n.of(crlfaslf));
                tableI18n.original2text().put(original, lang);
            }
        }
        return textI18n;
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

