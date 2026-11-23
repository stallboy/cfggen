package configgen.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 行为驱动测试：验证 HeadRow 和 HeadRows 类的公共行为
 * 专注于表头解析、类型推断、数值解析等外部行为
 */
class HeadRowsBehaviorTest {

    @Test
    void shouldReturnCorrectHeadRowInstanceWhenValidIdIsProvided() {
        // Given: 有效的 HeadRow ID
        String id2 = "2";
        String id3 = "3";

        // When: 通过 ID 获取 HeadRow 实例
        HeadRow row2 = HeadRows.getById(id2);
        HeadRow row3 = HeadRows.getById(id3);

        // Then: 应该返回正确的 HeadRow 实例
        assertSame(HeadRows.A2_Default, row2, "ID '2' 应该返回 A2_Default");
        assertSame(HeadRows.A3, row3, "ID '3' 应该返回 A3");
    }

    @Test
    void shouldThrowExceptionWhenInvalidHeadRowIdIsProvided() {
        // Given: 无效的 HeadRow ID
        String invalidId = "4";

        // When & Then: 应该抛出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            HeadRows.getById(invalidId);
        }, "无效的 ID 应该抛出异常");
    }

    @Test
    void shouldReturnCorrectRowCountForEachHeadRowType() {
        // When & Then: 验证每个 HeadRow 类型的行数
        assertEquals(2, HeadRows.A2_Default.rowCount(), "A2_Default 应该有 2 行");
        assertEquals(3, HeadRows.A3.rowCount(), "A3 应该有 3 行");
    }

    @Test
    void shouldReturnCorrectRowPositionsForA2DefaultHeadRow() {
        // Given: A2_Default HeadRow
        HeadRow headRow = HeadRows.A2_Default;

        // When & Then: 验证行位置
        assertEquals(0, headRow.commentRow(), "注释行应该在位置 0");
        assertEquals(1, headRow.nameRow(), "名称行应该在位置 1");
    }

    @Test
    void shouldParseValidLongValuesCorrectlyForAllHeadRowTypes() {
        // Given: 各种有效的长整数值
        String decimalValue = "123";
        String hexValue = "0x7B";
        String leadingZeroValue = "0173";
        String largeValue = "9223372036854775807";

        // When & Then: 验证所有 HeadRow 类型都能正确解析
        for (HeadRow headRow : new HeadRow[]{HeadRows.A2_Default, HeadRows.A3}) {
            assertEquals(123L, headRow.parseLong(decimalValue), "应该正确解析十进制值");
            assertEquals(123L, headRow.parseLong(hexValue), "应该正确解析十六进制值");
            assertEquals(173L, headRow.parseLong(leadingZeroValue), "前导 0 的值应该按十进制解析");
            assertEquals(9223372036854775807L, headRow.parseLong(largeValue), "应该正确解析大数值");
        }
    }

    @Test
    void shouldParseLongValuesWithAsteriskPrefixCorrectly() {
        // Given: 以 * 开头的长整数值（避免 Excel 科学计数法问题）
        String valueWithAsterisk = "*123456789012345";

        // When & Then: 验证所有 HeadRow 类型都能正确解析
        for (HeadRow headRow : new HeadRow[]{HeadRows.A2_Default, HeadRows.A3}) {
            assertEquals(123456789012345L, headRow.parseLong(valueWithAsterisk),
                "应该正确解析带 * 前缀的值");
        }
    }

    @Test
    void shouldReturnZeroWhenParsingEmptyStringAsLong() {
        // Given: 空字符串
        String emptyValue = "";

        // When & Then: 验证所有 HeadRow 类型都能正确处理空值
        for (HeadRow headRow : new HeadRow[]{HeadRows.A2_Default, HeadRows.A3}) {
            assertEquals(0L, headRow.parseLong(emptyValue), "空字符串应该解析为 0");
        }
    }

    @Test
    void shouldParseBooleanValuesCorrectlyForDefaultHeadRows() {
        // Given: 默认 HeadRow
        HeadRow defaultHeadRow = HeadRows.A2_Default;

        // When & Then: 验证布尔值解析
        assertEquals(HeadRow.ParseBoolResult.TRUE, defaultHeadRow.parseBool("1"), "'1' 应该解析为 TRUE");
        assertEquals(HeadRow.ParseBoolResult.TRUE, defaultHeadRow.parseBool("true"), "'true' 应该解析为 TRUE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, defaultHeadRow.parseBool("0"), "'0' 应该解析为 FALSE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, defaultHeadRow.parseBool("false"), "'false' 应该解析为 FALSE");
        assertEquals(HeadRow.ParseBoolResult.INVALID, defaultHeadRow.parseBool("invalid"), "无效值应该返回 INVALID");
    }

    @Test
    void shouldHandleNullAndEmptyBooleanValuesCorrectly() {
        // Given: 默认 HeadRow
        HeadRow defaultHeadRow = HeadRows.A2_Default;

        // When & Then: 验证 null 和空字符串的处理
        assertEquals(HeadRow.ParseBoolResult.FALSE, defaultHeadRow.parseBool(null), "null 应该解析为 FALSE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, defaultHeadRow.parseBool(""), "空字符串应该解析为 FALSE");
    }

    @Test
    void shouldReturnCorrectParseBoolResultForCaseInsensitiveBooleanValues() {
        // Given: 默认 HeadRow 和大小写混合的布尔值
        HeadRow defaultHeadRow = HeadRows.A2_Default;

        // When & Then: 验证大小写不敏感的布尔值解析
        assertEquals(HeadRow.ParseBoolResult.TRUE, defaultHeadRow.parseBool("True"), "'True' 应该解析为 TRUE");
        assertEquals(HeadRow.ParseBoolResult.TRUE, defaultHeadRow.parseBool("TRUE"), "'TRUE' 应该解析为 TRUE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, defaultHeadRow.parseBool("False"), "'False' 应该解析为 FALSE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, defaultHeadRow.parseBool("FALSE"), "'FALSE' 应该解析为 FALSE");
    }
}