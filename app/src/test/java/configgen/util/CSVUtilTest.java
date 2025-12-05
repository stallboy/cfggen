package configgen.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVUtilTest {

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
