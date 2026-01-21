package configgen.schema.cfg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommentUtils 工具类的单元测试
 */
class CommentUtilsTest {

    @Test
    void testParseComment_OnlyTrailing() {
        // 只有行尾注释
        CommentUtils.ParsedComment result = CommentUtils.parseComment("这是行尾注释");
        assertEquals("", result.leading());
        assertEquals("这是行尾注释", result.trailing());
    }

    @Test
    void testParseComment_OnlyLeading() {
        // 只有声明前注释
        CommentUtils.ParsedComment result = CommentUtils.parseComment("第一行\n第二行\n第三行");
        assertEquals("第一行\n第二行\n第三行", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testParseComment_BothLeadingAndTrailing() {
        // 同时有声明前注释和行尾注释
        CommentUtils.ParsedComment result = CommentUtils.parseComment("声明前注释第一行\n声明前注释第二行\n>>>\n行尾注释");
        assertEquals("声明前注释第一行\n声明前注释第二行", result.leading());
        assertEquals("行尾注释", result.trailing());
    }

    @Test
    void testParseComment_EmptyString() {
        // 空字符串
        CommentUtils.ParsedComment result = CommentUtils.parseComment("");
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testParseComment_Null() {
        // null 输入
        CommentUtils.ParsedComment result = CommentUtils.parseComment(null);
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testParseComment_MultipleSeparators() {
        // 多个分隔符，应该只在第一个处分割
        CommentUtils.ParsedComment result = CommentUtils.parseComment("第一行\n>>>\n第二行\n>>>\n第三行");
        assertEquals("第一行", result.leading());
        assertEquals("第二行\n>>>\n第三行", result.trailing());
    }

    @Test
    void testParseComment_SeparatorOnly() {
        // 只有分隔符
        CommentUtils.ParsedComment result = CommentUtils.parseComment("\n>>>\n");
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testParsedCommentFormatLeading_SingleLine() {
        // 格式化单行声明前注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("这是注释", "");
        String result = pc.formatLeading("  ");
        assertEquals("  // 这是注释\r\n", result);
    }

    @Test
    void testParsedCommentFormatLeading_MultipleLines() {
        // 格式化多行声明前注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("第一行\n第二行\n第三行", "");
        String result = pc.formatLeading("\t");
        assertEquals("\t// 第一行\r\n" +
                     "\t// 第二行\r\n" +
                     "\t// 第三行\r\n", result);
    }

    @Test
    void testParsedCommentFormatLeading_Empty() {
        // 空声明前注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("", "行尾注释");
        String result = pc.formatLeading("  ");
        assertEquals("", result);
    }

    @Test
    void testParsedCommentFormatLeading_LinesWithExtraSpaces() {
        // 行首尾有空格，应该被trim
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("  第一行  \n  第二行  ", "");
        String result = pc.formatLeading("  ");
        assertEquals("  // 第一行\r\n" +
                     "  // 第二行\r\n", result);
    }

    @Test
    void testParsedCommentFormatLeading_EmptyLines() {
        // 包含空行
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("第一行\n\n第二行", "");
        String result = pc.formatLeading("  ");
        assertEquals("  // 第一行\r\n" +
                     "  // 第二行\r\n", result);
    }

    @Test
    void testParsedCommentFormatTrailing_Normal() {
        // 格式化行尾注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("", "这是行尾注释");
        String result = pc.formatTrailing();
        assertEquals(" // 这是行尾注释", result);
    }

    @Test
    void testParsedCommentFormatTrailing_Empty() {
        // 空行尾注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("声明前注释", "");
        String result = pc.formatTrailing();
        assertEquals("", result);
    }

    @Test
    void testParsedCommentFormatTrailing_BothHaveContent() {
        // 两者都有内容
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("声明前", "行尾");
        assertEquals(" // 行尾", pc.formatTrailing());
        assertEquals("  // 声明前\r\n", pc.formatLeading("  "));
    }

    @Test
    void testParsedCommentRoundTrip() {
        // 测试往返一致性
        String original = "声明前第一行\n声明前第二行\n>>>\n行尾注释";
        CommentUtils.ParsedComment pc = CommentUtils.parseComment(original);

        // 验证解析正确
        assertEquals("声明前第一行\n声明前第二行", pc.leading());
        assertEquals("行尾注释", pc.trailing());

        // 验证格式化
        String formattedLeading = pc.formatLeading("");
        assertEquals("// 声明前第一行\r\n// 声明前第二行\r\n", formattedLeading);

        String formattedTrailing = pc.formatTrailing();
        assertEquals(" // 行尾注释", formattedTrailing);
    }
}
