package configgen.schema.cfg;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public final class CommentUtils {

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

    public static CommentData decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return new CommentData("", "", "");
        }

        // 先用 DELIMITER2 ("<<<") 分离 suffix
        String mainPart;
        String suffix;
        int idx2 = raw.indexOf(CommentData.DELIMITER2);
        if (idx2 >= 0) {
            mainPart = raw.substring(0, idx2);
            suffix = raw.substring(idx2 + CommentData.DELIMITER2.length());
        } else {
            mainPart = raw;
            suffix = "";
        }

        // 再用 DELIMITER1 (">>>") 分离 leading 和 trailing
        String leading;
        String trailing;
        int idx1 = mainPart.indexOf(CommentData.DELIMITER1);
        if (idx1 >= 0) {
            leading = mainPart.substring(0, idx1);
            trailing = mainPart.substring(idx1 + CommentData.DELIMITER1.length());
        } else {
            // 这里是猜测：包含\n就是leading，不包含就是trailing
            if (mainPart.contains("\n")) {
                leading = mainPart;
                if (leading.endsWith("\n")) {
                    String maybeLeading = leading.substring(0, leading.length() - 1);
                    if (!maybeLeading.contains("\n")) {
                        leading = maybeLeading;
                    }
                }
                trailing = "";
            } else {
                leading = "";
                trailing = mainPart;
            }
        }

        return new CommentData(leading, trailing, suffix);
    }

    public static @NotNull String readFull3(List<CfgParser.Leading_commentContext> leadingContexts,
                                            TerminalNode trailingNode,
                                            List<CfgParser.Suffix_commentContext> suffixContexts) {
        String leading = readLeadingComment(leadingContexts);
        String trailing = readTrailingComment(trailingNode);
        String suffix = readSuffixComment(suffixContexts);
        return new CommentData(leading, trailing, suffix).encode();
    }

    public static @NotNull String readFull2(List<CfgParser.Leading_commentContext> leadingContexts,
                                            TerminalNode trailingNode) {
        String leading = readLeadingComment(leadingContexts);
        String trailing = readTrailingComment(trailingNode);
        return new CommentData(leading, trailing, null).encode();
    }

    /**
     * 从 Leading_commentContext 列表中提取声明前注释
     */
    private static String readLeadingComment(List<CfgParser.Leading_commentContext> leadingComments) {
        if (leadingComments == null || leadingComments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CfgParser.Leading_commentContext lc : leadingComments) {
            if (lc.COMMENT() != null) {
                String text = lc.COMMENT().getText();
                if (text.startsWith("//")) {
                    String commentText = text.substring(2).trim();
                    if (!commentText.isEmpty()) {
                        if (!sb.isEmpty())
                            sb.append("\n");
                        sb.append(commentText);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * 从 LC_COMMENT 或 SEMI_COMMENT token 中提取注释
     * tokenText 格式: "{ // comment" 或 "; // comment" 或 "{" 或 ";"
     */
    private static String readTrailingComment(TerminalNode trailingNode) {
        String tokenText = trailingNode.getText();
        if (tokenText == null || tokenText.isEmpty()) {
            return "";
        }
        int commentIndex = tokenText.indexOf("//");
        if (commentIndex >= 0) {
            return tokenText.substring(commentIndex + 2).trim();
        }
        return "";
    }

    /**
     * 从 Suffix_commentContext 列表中提取后缀注释
     */
    public static String readSuffixComment(List<CfgParser.Suffix_commentContext> suffixComments) {
        if (suffixComments == null || suffixComments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (CfgParser.Suffix_commentContext sc : suffixComments) {
            if (sc.COMMENT() != null) {
                String text = sc.COMMENT().getText();
                if (text.startsWith("//")) {
                    String commentText = text.substring(2).trim();
                    if (!commentText.isEmpty()) {
                        if (!sb.isEmpty())
                            sb.append("\n");
                        sb.append(commentText);
                    }
                }
            }
        }
        return sb.toString();
    }

}
