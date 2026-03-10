package configgen.schema.cfg;

import configgen.schema.cfg.CommentUtils.CommentData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommentUtils 工具类的单元测试
 */
class CommentUtilsTest {

    @Test
    void testDecode_OnlyTrailing() {
        // 只有行尾注释
        CommentData result = CommentUtils.decode("这是行尾注释");
        assertEquals("", result.leading());
        assertEquals("这是行尾注释", result.trailing());
    }

    @Test
    void testDecode_OnlyLeading() {
        // 只有声明前注释
        CommentData result = CommentUtils.decode("第一行\n第二行\n第三行");
        assertEquals("第一行\n第二行\n第三行", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testDecode_BothLeadingAndTrailing() {
        // 同时有声明前注释和行尾注释
        CommentData result = CommentUtils.decode("声明前注释第一行\n声明前注释第二行>>>行尾注释");
        assertEquals("声明前注释第一行\n声明前注释第二行", result.leading());
        assertEquals("行尾注释", result.trailing());
    }

    @Test
    void testDecode_EmptyString() {
        // 空字符串
        CommentData result = CommentUtils.decode("");
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testDecode_Null() {
        // null 输入
        CommentData result = CommentUtils.decode(null);
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
    }

    @Test
    void testDecode_MultipleSeparators() {
        // 多个分隔符，应该只在第一个处分割
        CommentData result = CommentUtils.decode("第一行>>>第二行>>>第三行");
        assertEquals("第一行", result.leading());
        assertEquals("第二行>>>第三行", result.trailing());
    }

    @Test
    void testParsedCommentFormatLeading_SingleLine() {
        // 格式化单行声明前注释
        CommentData pc = new CommentData("这是注释", "", "");
        String result = pc.formatLeading("  ");
        assertEquals("  // 这是注释\n", result);
    }

    @Test
    void testParsedCommentFormatLeading_MultipleLines() {
        // 格式化多行声明前注释
        CommentData pc = new CommentData("第一行\n第二行\n第三行", "", "");
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
        CommentData pc = new CommentData("", "行尾注释", "");
        String result = pc.formatLeading("  ");
        assertEquals("", result);
    }

    @Test
    void testParsedCommentFormatLeading_LinesWithExtraSpaces() {
        // 行首尾有空格，应该被trim
        CommentData pc = new CommentData("  第一行  \n  第二行  ", "", "");
        String result = pc.formatLeading("  ");
        assertEquals("""
                  // 第一行
                  // 第二行
                """, result);
    }

    @Test
    void testParsedCommentFormatLeading_EmptyLines() {
        // 包含空行
        CommentData pc = new CommentData("第一行\n\n第二行", "", "");
        String result = pc.formatLeading("  ");
        assertEquals("""
                  // 第一行
                  // 第二行
                """, result);
    }

    @Test
    void testParsedCommentFormatTrailing_Normal() {
        // 格式化行尾注释
        CommentData pc = new CommentData("", "这是行尾注释", "");
        String result = pc.formatTrailing();
        assertEquals(" // 这是行尾注释", result);
    }

    @Test
    void testParsedCommentFormatTrailing_Empty() {
        // 空行尾注释
        CommentData pc = new CommentData("声明前注释", "", "");
        String result = pc.formatTrailing();
        assertEquals("", result);
    }

    @Test
    void testParsedCommentFormatTrailing_BothHaveContent() {
        // 两者都有内容
        CommentData pc = new CommentData("声明前", "行尾", "");
        assertEquals(" // 行尾", pc.formatTrailing());
        assertEquals("  // 声明前\n", pc.formatLeading("  "));
    }

    @Test
    void testParsedCommentRoundTrip() {
        // 测试往返一致性
        String original = "声明前第一行\n声明前第二行>>>行尾注释";
        CommentData pc = CommentUtils.decode(original);

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
    void testDecode_WithSuffix() {
        // 有后缀注释
        CommentData result = CommentUtils.decode("声明前>>>行尾<<<后缀第一行\n后缀第二行");
        assertEquals("声明前", result.leading());
        assertEquals("行尾", result.trailing());
        assertEquals("后缀第一行\n后缀第二行", result.suffix());
    }

    @Test
    void testDecode_OnlySuffix() {
        // 只有后缀注释
        CommentData result = CommentUtils.decode("<<<后缀注释");
        assertEquals("", result.leading());
        assertEquals("", result.trailing());
        assertEquals("后缀注释", result.suffix());
    }

    @Test
    void testDecode_TrailingAndSuffix() {
        // 行尾和后缀注释
        CommentData result = CommentUtils.decode("行尾<<<后缀注释");
        assertEquals("", result.leading());
        assertEquals("行尾", result.trailing());
        assertEquals("后缀注释", result.suffix());
    }

    @Test
    void testParsedCommentFormatSuffix_SingleLine() {
        // 格式化单行后缀注释（formatSuffix 会在 prefix 后额外添加一个 \t）
        CommentData pc = new CommentData("", "", "后缀注释");
        String result = pc.formatSuffix("\t");
        assertEquals("\t\t// 后缀注释\n", result);
    }

    @Test
    void testParsedCommentFormatSuffix_MultipleLines() {
        // 格式化多行后缀注释（formatSuffix 会在 prefix 后额外添加一个 \t）
        CommentData pc = new CommentData("", "", "后缀第一行\n后缀第二行");
        String result = pc.formatSuffix("\t");
        assertEquals("\t\t// 后缀第一行\n\t\t// 后缀第二行\n", result);
    }

    @Test
    void testParsedCommentFormatSuffix_Empty() {
        // 空后缀注释
        CommentData pc = new CommentData("声明前", "行尾", "");
        String result = pc.formatSuffix("\t");
        assertEquals("", result);
    }

}
