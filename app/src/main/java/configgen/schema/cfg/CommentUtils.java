package configgen.schema.cfg;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 注释处理工具类
 * <p>
 * 用于处理声明前注释和行尾注释的解析、格式化和合并。
 * 注释使用 "\n&gt;&gt;&gt;\n" 作为分隔符来分隔声明前注释和行尾注释。
 */
public final class CommentUtils {

    /**
     * 解析后的注释，包含声明前注释和行尾注释两部分
     *
     * @param leading 声明前注释（多行）
     * @param trailing 行尾注释（单行）
     */
    public record ParsedComment(@NotNull String leading,
                                @NotNull String trailing) {


        public String formatLeading(String prefix) {
            if (leading.isEmpty()) {
                return "";
            }
            String[] lines = leading.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    sb.append(prefix).append("// ").append(line).append("\r\n");
                }
            }
            return sb.toString();
        }


        public String formatTrailing() {
            if (trailing.isEmpty()) {
                return "";
            }
            return " // " + trailing;
        }

    }

    /**
     * 解析注释字符串，拆分为声明前注释和行尾注释
     * <p>
     * 注释格式：
     * <ul>
     *   <li>仅声明前注释: "line1\nline2" (包含换行符)</li>
     *   <li>仅行尾注释: "trailing comment" (不包含换行符)</li>
     *   <li>两者都有: "line1\nline2\n&gt;&gt;&gt;\ntrailing comment"</li>
     * </ul>
     *
     * @param comment 原始注释字符串
     * @return 解析后的注释，如果输入为空则返回两个空字符串
     */
    public static ParsedComment parseComment(String comment) {
        if (comment == null || comment.isEmpty()) {
            return new ParsedComment("", "");
        }

        String[] parts = comment.split("\n>>>\n", 2);
        if (parts.length == 2) {
            return new ParsedComment(parts[0], parts[1]);
        }

        // 没有分隔符，根据是否包含换行符来判断类型
        if (comment.contains("\n")) {
            // 包含换行符，认为是声明前注释
            return new ParsedComment(comment, "");
        } else {
            // 不包含换行符，认为是行尾注释
            return new ParsedComment("", comment);
        }
    }


    /**
     * 合并声明前注释和行尾注释，用 \n&gt;&gt;&gt;\n 分隔
     *
     * @param leadingComments 声明前注释上下文列表
     * @param trailingComment 行尾注释文本（已提取，不含 "//" 前缀）
     * @return 合并后的注释字符串
     */
    public static String buildComment(
            List<configgen.schema.cfg.CfgParser.CommentContext> leadingComments,
            String trailingComment) {

        String leading = mergeLeadingComments(leadingComments);

        if (leading.isEmpty()) {
            return trailingComment;
        } else {
            return leading + "\n>>>\n" + trailingComment;
        }
    }

    private static String mergeLeadingComments(
            List<configgen.schema.cfg.CfgParser.CommentContext> commentContexts) {
        if (commentContexts == null || commentContexts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (configgen.schema.cfg.CfgParser.CommentContext cc : commentContexts) {
            if (cc.COMMENT() != null) {
                String text = extractCommentText(cc.COMMENT());
                if (!text.isEmpty()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }

    private static String extractCommentText(org.antlr.v4.runtime.tree.TerminalNode commentNode) {
        if (commentNode == null) {
            return "";
        }
        String text = commentNode.getText();
        if (text.startsWith("//")) {
            return text.substring(2).trim();
        }
        return "";
    }


}
