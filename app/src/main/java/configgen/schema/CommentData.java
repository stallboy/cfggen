package configgen.schema;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * 封装注释的三部分数据
 */
public record CommentData(@NotNull String leading,
                          @NotNull String trailing,
                          String suffix) {

    public CommentData {
        if (trailing.contains("\n")) { // 不会发生，这里以防万一
            trailing = trailing.replace("\n", "LF");
        }
    }

    public static final String DELIMITER1 = ">>>";
    public static final String DELIMITER2 = "<<<";

    public String encode() {
        if (leading.isBlank() && trailing.isBlank() && (suffix == null || suffix.isBlank())) {
            return "";
        }

        String res;
        if (leading.isBlank()) {
            res = trailing;
        } else if (trailing.isBlank()) {
            res = leading;
            if (!res.contains("\n")) { // 加一个\n来让decode区分出它是leading
                res += "\n";
            }
        } else {
            res = leading + DELIMITER1 + trailing;
        }

        if (suffix != null && !suffix.isBlank()) {
            res += DELIMITER2 + suffix;
        }
        return res;
    }

    public String formatLeading(String prefix) {
        return formatLines(leading, prefix + "// ");
    }

    public String formatTrailing() {
        return trailing.isBlank() ? "" : " // " + trailing;
    }

    public String formatSuffix(String prefix) {
        return suffix != null ? formatLines(suffix, prefix + "\t// ") : "";
    }

    private String formatLines(String text, String linePrefix) {
        if (text.isBlank())
            return "";
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> linePrefix + line + "\n")
                .collect(Collectors.joining());
    }
}
