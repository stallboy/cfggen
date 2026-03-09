package configgen.schema.cfg;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CommentUtils {

    static @NotNull String readFullComment(String tokenText,
                                                   List<CfgParser.Leading_commentContext> leadingContexts,
                                                   List<CfgParser.Suffix_commentContext> suffixContexts) {
        String trailing = readTrailingComment(tokenText);
        String leading = readLeadingComment(leadingContexts);
        String suffix = readSuffixComment(suffixContexts);
        return new CommentData(leading, trailing, suffix).encode();
    }

    /**
     * 从 LC_COMMENT 或 SEMI_COMMENT token 中提取注释
     * tokenText 格式: "{ // comment" 或 "; // comment" 或 "{" 或 ";"
     */
    private static String readTrailingComment(String tokenText) {
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
    private static String readSuffixComment(List<CfgParser.Suffix_commentContext> suffixComments) {
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
                        if (!sb.isEmpty()) sb.append("\n");
                        sb.append(commentText);
                    }
                }
            }
        }
        return sb.toString();
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
                        if (!sb.isEmpty()) sb.append("\n");
                        sb.append(commentText);
                    }
                }
            }
        }
        return sb.toString();
    }






    /**
     * 合并声明前注释和行尾注释（兼容旧版，无后缀注释）
     *
     * @param leadingComments 声明前注释上下文列表（使用 CommentContext）
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

    /**
     * 合并 Leading_commentContext 列表为字符串
     */
    public static String mergeLeadingCommentsFromLeadingContext(
            List<configgen.schema.cfg.CfgParser.Leading_commentContext> commentContexts) {
        if (commentContexts == null || commentContexts.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (configgen.schema.cfg.CfgParser.Leading_commentContext lc : commentContexts) {
            if (lc.COMMENT() != null) {
                String text = extractCommentText(lc.COMMENT());
                if (!text.isEmpty()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(text);
                }
            }
        }
        return sb.toString();
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
