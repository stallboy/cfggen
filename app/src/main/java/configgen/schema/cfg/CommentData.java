package configgen.schema.cfg;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * 封装注释的三部分数据
 */
public record CommentData(@NotNull String leading,
                          @NotNull String trailing,
                          @NotNull String suffix) {

    private static final String DELIMITER = "\u001E";

    public static CommentData decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CommentData("", "", "");
        }
        // 按分隔符严格拆分，传入 -1 保证保留末尾的空字符串
        String[] parts = raw.split(DELIMITER, -1);
        String l = parts.length > 0 ? parts[0] : "";
        String t = parts.length > 1 ? parts[1] : "";
        String s = parts.length > 2 ? parts[2] : "";

        return new CommentData(l, t, s);
    }

    public String encode() {
        if (leading.isEmpty() && trailing.isEmpty() && suffix.isEmpty()) {
            return "";
        }
        // 严格占位：leading + 分隔符 + trailing + 分隔符 + suffix
        return leading + DELIMITER + trailing + DELIMITER + suffix;
    }

    public String formatLeading(String prefix) {
        return formatLines(leading, prefix + "// ");
    }

    public String formatTrailing() {
        return trailing.isBlank() ? "" : " // " + trailing;
    }

    public String formatSuffix(String prefix) {
        return formatLines(suffix, prefix + "\t// ");
    }

    private String formatLines(String text, String linePrefix) {
        if (text.isBlank()) return "";
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> linePrefix + line + "\n")
                .collect(Collectors.joining());
    }
}
