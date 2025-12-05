package configgen.geni18n;

import configgen.util.CSVUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TermUpdaterTest {

    @Test
    void testParseMarkdownTable() {
        // 使用 term.md 中的示例数据
        String markdown = """
                | 原文术语 | 目标语言术语 | 类别 | confidence | 备注 |
                | :--- | :--- | :--- | :--- | :--- |
                | 游侠 | Ranger | 角色 | high | 职业名称。 |
                | 野蛮人 | Barbarian | 角色 | high | 职业名称。 |
                | 法师 | Mage | 角色 | high | 职业名称。 |
                | 进阶 | Intermediate | 系统 | high | 推荐类型标签。 |
                | 新 | New | 系统 | high | 推荐类型标签。 |
                """;

        List<TermFile.TermEntry> entries = TermUpdater.parseMarkdownTable(markdown);

        assertEquals(5, entries.size());

        // 验证第一个条目
        TermFile.TermEntry first = entries.get(0);
        assertEquals("游侠", first.original());
        assertEquals("Ranger", first.translated());
        assertEquals("角色", first.category());
        assertEquals("high", first.confidence());
        assertEquals("职业名称。", first.note());

        // 验证第二个条目
        TermFile.TermEntry second = entries.get(1);
        assertEquals("野蛮人", second.original());
        assertEquals("Barbarian", second.translated());
        assertEquals("角色", second.category());
        assertEquals("high", second.confidence());
        assertEquals("职业名称。", second.note());

        // 验证最后一个条目
        TermFile.TermEntry last = entries.get(4);
        assertEquals("新", last.original());
        assertEquals("New", last.translated());
        assertEquals("系统", last.category());
        assertEquals("high", last.confidence());
        assertEquals("推荐类型标签。", last.note());
    }

    @Test
    void testParseMarkdownTableWithEmptyCells() {
        String markdown = """
                | 原文术语 | 目标语言术语 | 类别 | confidence | 备注 |
                | :--- | :--- | :--- | :--- | :--- |
                | 测试1 | Test1 | 类别1 | high | 备注1 |
                | 测试2 | Test2 |  | medium |  |
                | 测试3 | Test3 | 类别3 |  | 备注3 |
                """;

        List<TermFile.TermEntry> entries = TermUpdater.parseMarkdownTable(markdown);

        assertEquals(3, entries.size());

        // 验证第二个条目（空类别和备注）
        TermFile.TermEntry second = entries.get(1);
        assertEquals("测试2", second.original());
        assertEquals("Test2", second.translated());
        assertEquals("", second.category());
        assertEquals("medium", second.confidence());
        assertEquals("", second.note());

        // 验证第三个条目（空 confidence）
        TermFile.TermEntry third = entries.get(2);
        assertEquals("测试3", third.original());
        assertEquals("Test3", third.translated());
        assertEquals("类别3", third.category());
        assertEquals("", third.confidence());
        assertEquals("备注3", third.note());
    }

    @Test
    void testParseMarkdownTableWithSpecialCharacters() {
        String markdown = """
                | 原文术语 | 目标语言术语 | 类别 | confidence | 备注 |
                | :--- | :--- | :--- | :--- | :--- |
                | 测试,逗号 | Test,Comma | 类别 | high | 备注,包含逗号 |
                | 测试"引号 | Test"Quote | 类别 | high | 备注"包含引号 |
                | 测试\\n换行 | Test\\nNewline | 类别 | high | 备注\\n包含换行 |
                """;

        List<TermFile.TermEntry> entries = TermUpdater.parseMarkdownTable(markdown);

        assertEquals(3, entries.size());

        // 验证包含逗号的条目
        TermFile.TermEntry first = entries.get(0);
        assertEquals("测试,逗号", first.original());
        assertEquals("Test,Comma", first.translated());
        assertEquals("类别", first.category());
        assertEquals("high", first.confidence());
        assertEquals("备注,包含逗号", first.note());
    }

    @Test
    void testParseMarkdownTableWithTableHeaders() {
        // 测试包含表头行的 Markdown（如 term.md 中的格式）
        String markdown = """
                ### 提取的术语表

                | 原文术语 | 目标语言术语 | 类别 | confidence | 备注 |
                | :--- | :--- | :--- | :--- | :--- |
                | **表：profession.profession** | | | | |
                | 游侠 | Ranger | 角色 | high | 职业名称。 |
                | 野蛮人 | Barbarian | 角色 | high | 职业名称。 |
                | **表：mwrecommend.recommendtype** | | | | |
                | 进阶 | Intermediate | 系统 | high | 推荐类型标签。 |
                """;

        List<TermFile.TermEntry> entries = TermUpdater.parseMarkdownTable(markdown);

        // 应该只解析实际的数据行，跳过表头行和空行
        assertEquals(3, entries.size());

        TermFile.TermEntry first = entries.get(0);
        assertEquals("游侠", first.original());
        assertEquals("Ranger", first.translated());
        assertEquals("角色", first.category());
        assertEquals("high", first.confidence());
        assertEquals("职业名称。", first.note());
    }

    @Test
    void testEscapeCsv() {
        // 测试空值和空字符串
        assertEquals("", CSVUtil.escapeCsv(null));
        assertEquals("", CSVUtil.escapeCsv(""));

        // 测试普通字符串（不需要转义）
        assertEquals("hello", CSVUtil.escapeCsv("hello"));
        assertEquals("测试", CSVUtil.escapeCsv("测试"));

        // 测试包含逗号的字符串
        assertEquals("\"hello,world\"", CSVUtil.escapeCsv("hello,world"));
        assertEquals("\"测试,逗号\"", CSVUtil.escapeCsv("测试,逗号"));

        // 测试包含双引号的字符串
        assertEquals("\"hello\"\"world\"", CSVUtil.escapeCsv("hello\"world"));
        assertEquals("\"测试\"\"引号\"", CSVUtil.escapeCsv("测试\"引号"));

        // 测试同时包含逗号和引号的字符串
        assertEquals("\"hello,\"\"world\"\"\"", CSVUtil.escapeCsv("hello,\"world\""));
    }

}