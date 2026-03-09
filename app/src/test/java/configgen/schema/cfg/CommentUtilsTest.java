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
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("这是注释", "", "");
        String result = pc.formatLeading("  ");
        assertEquals("  // 这是注释\n", result);
    }

    @Test
    void testParsedCommentFormatLeading_MultipleLines() {
        // 格式化多行声明前注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("第一行\n第二行\n第三行", "", "");
        String result = pc.formatLeading("\t");
        assertEquals("""
                \t// 第一行
                \t// 第二行
                \t// 第三行
                """, result);
    }

    @Test
    void testParsedCommentFormatLeading_Empty() {
        // 空声明前注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("", "行尾注释", "");
        String result = pc.formatLeading("  ");
        assertEquals("", result);
    }

    @Test
    void testParsedCommentFormatLeading_LinesWithExtraSpaces() {
        // 行首尾有空格，应该被trim
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("  第一行  \n  第二行  ", "", "");
        String result = pc.formatLeading("  ");
        assertEquals("""
                  // 第一行
                  // 第二行
                """, result);
    }

    @Test
    void testParsedCommentFormatLeading_EmptyLines() {
        // 包含空行
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("第一行\n\n第二行", "", "");
        String result = pc.formatLeading("  ");
        assertEquals("""
                  // 第一行
                  // 第二行
                """, result);
    }

    @Test
    void testParsedCommentFormatTrailing_Normal() {
        // 格式化行尾注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("", "这是行尾注释", "");
        String result = pc.formatTrailing();
        assertEquals(" // 这是行尾注释", result);
    }

    @Test
    void testParsedCommentFormatTrailing_Empty() {
        // 空行尾注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("声明前注释", "", "");
        String result = pc.formatTrailing();
        assertEquals("", result);
    }

    @Test
    void testParsedCommentFormatTrailing_BothHaveContent() {
        // 两者都有内容
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("声明前", "行尾", "");
        assertEquals(" // 行尾", pc.formatTrailing());
        assertEquals("  // 声明前\n", pc.formatLeading("  "));
    }

    @Test
    void testParsedCommentRoundTrip() {
        // 测试往返一致性
        String original = "声明前第一行\n声明前第二行\n>>>\n行尾注释";
        CommentUtils.ParsedComment pc = CommentUtils.parseComment(original);

        // 验证解析正确
        assertEquals("声明前第一行\n声明前第二行", pc.leading());
        assertEquals("行尾注释", pc.trailing());
        assertEquals("", pc.suffix());

        // 验证格式化
        String formattedLeading = pc.formatLeading("");
        assertEquals("// 声明前第一行\n// 声明前第二行\n", formattedLeading);

        String formattedTrailing = pc.formatTrailing();
        assertEquals(" // 行尾注释", formattedTrailing);
    }

    @Test
    void testParseComment_WithSuffix() {
        // 有后缀注释
        CommentUtils.ParsedComment result = CommentUtils.parseComment("声明前\n>>>\n行尾\n<<<\n后缀第一行\n后缀第二行");
        assertEquals("声明前", result.leading());
        assertEquals("行尾", result.trailing());
        assertEquals("后缀第一行\n后缀第二行", result.suffix());
    }

    @Test
    void testParseComment_OnlySuffix() {
        // 只有后缀注释
        CommentUtils.ParsedComment result = CommentUtils.parseComment("\n<<<\n后缀注释");
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
        assertEquals("后缀注释", result.suffix());
    }

    @Test
    void testParseComment_TrailingAndSuffix() {
        // 行尾和后缀注释
        CommentUtils.ParsedComment result = CommentUtils.parseComment("行尾\n<<<\n后缀注释");
        assertEquals("", result.leading());
        assertEquals("行尾", result.trailing());
        assertEquals("后缀注释", result.suffix());
    }

    @Test
    void testParsedCommentFormatSuffix_SingleLine() {
        // 格式化单行后缀注释（formatSuffix 会在 prefix 后额外添加一个 \t）
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("", "", "后缀注释");
        String result = pc.formatSuffix("\t");
        assertEquals("\t\t// 后缀注释\n", result);
    }

    @Test
    void testParsedCommentFormatSuffix_MultipleLines() {
        // 格式化多行后缀注释（formatSuffix 会在 prefix 后额外添加一个 \t）
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("", "", "后缀第一行\n后缀第二行");
        String result = pc.formatSuffix("\t");
        assertEquals("\t\t// 后缀第一行\n\t\t// 后缀第二行\n", result);
    }

    @Test
    void testParsedCommentFormatSuffix_Empty() {
        // 空后缀注释
        CommentUtils.ParsedComment pc = new CommentUtils.ParsedComment("声明前", "行尾", "");
        String result = pc.formatSuffix("\t");
        assertEquals("", result);
    }

    @Test
    void testBuildCommentFromStrings_AllParts() {
        // 测试构建完整注释字符串
        String result = CommentUtils.buildCommentFromStrings("声明前", "行尾", "后缀");
        assertEquals("声明前\n>>>\n行尾\n<<<\n后缀", result);
    }

    @Test
    void testBuildCommentFromStrings_OnlyLeading() {
        // 只有声明前注释（需要添加 >>> 标记以便识别为 leading）
        String result = CommentUtils.buildCommentFromStrings("声明前", "", "");
        assertEquals("声明前\n>>>\n", result);
    }

    @Test
    void testBuildCommentFromStrings_OnlyTrailing() {
        // 只有行尾注释
        String result = CommentUtils.buildCommentFromStrings("", "行尾", "");
        assertEquals("行尾", result);
    }

    @Test
    void testBuildCommentFromStrings_OnlySuffix() {
        // 只有后缀注释
        String result = CommentUtils.buildCommentFromStrings("", "", "后缀");
        assertEquals("后缀", result);
    }

    @Test
    void testBuildCommentFromStrings_TrailingAndSuffix() {
        // 行尾和后缀注释
        String result = CommentUtils.buildCommentFromStrings("", "行尾", "后缀");
        assertEquals("行尾\n<<<\n后缀", result);
    }
}
