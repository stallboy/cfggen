package configgen.ctx;

import java.util.Map;
import java.util.regex.Pattern;

public record TextI18n(Map<String, TableI18n> tableI18nMap) {
    public TableI18n getTableI18n(String table) {
        return tableI18nMap.get(table);
    }

    public record TableI18n(Map<String, String> original2text,
                            boolean isCRLFAsLF) {
        public String findText(String original) {
            String normalized = normalize(original, isCRLFAsLF);
            String text = original2text.get(normalized);
            if (text != null && !text.isEmpty()) {
                return text;
            }
            return null;
        }
    }

    private static final Pattern pattern = Pattern.compile("\r\n");

    public static String normalize(String text, boolean isCRLFAsLF) {
        if (isCRLFAsLF) {
            return pattern.matcher(text).replaceAll("\n");
        } else {
            return text;
        }
    }
}

