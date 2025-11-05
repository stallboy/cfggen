package configgen.ctx;

import configgen.schema.FieldType;
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
        String id4 = "4";

        // When: 通过 ID 获取 HeadRow 实例
        HeadRow row2 = HeadRows.getById(id2);
        HeadRow row3 = HeadRows.getById(id3);
        HeadRow row4 = HeadRows.getById(id4);

        // Then: 应该返回正确的 HeadRow 实例
        assertSame(HeadRows.A2_Default, row2, "ID '2' 应该返回 A2_Default");
        assertSame(HeadRows.A3, row3, "ID '3' 应该返回 A3");
        assertSame(HeadRows.A4, row4, "ID '4' 应该返回 A4");
    }

    @Test
    void shouldThrowExceptionWhenInvalidHeadRowIdIsProvided() {
        // Given: 无效的 HeadRow ID
        String invalidId = "5";

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
        assertEquals(4, HeadRows.A4.rowCount(), "A4 应该有 4 行");
    }

    @Test
    void shouldReturnCorrectRowPositionsForA2DefaultHeadRow() {
        // Given: A2_Default HeadRow
        HeadRow headRow = HeadRows.A2_Default;

        // When & Then: 验证行位置
        assertEquals(0, headRow.commentRow(), "注释行应该在位置 0");
        assertEquals(1, headRow.nameRow(), "名称行应该在位置 1");
        assertEquals(-1, headRow.suggestedTypeRow(), "类型建议行不应该存在");
    }

    @Test
    void shouldReturnCorrectRowPositionsForA4HeadRow() {
        // Given: A4 HeadRow
        HeadRow headRow = HeadRows.A4;

        // When & Then: 验证行位置
        assertEquals(3, headRow.commentRow(), "注释行应该在位置 3");
        assertEquals(0, headRow.nameRow(), "名称行应该在位置 0");
        assertEquals(1, headRow.suggestedTypeRow(), "类型建议行应该在位置 1");
    }

    @Test
    void shouldParseStringTypeForDefaultHeadRowsWhenTypeIsNotSpecified() {
        // Given: 默认 HeadRow 和未指定的类型
        HeadRow defaultHeadRow = HeadRows.A2_Default;
        String unspecifiedType = "";

        // When: 解析类型
        FieldType result = defaultHeadRow.parseType(unspecifiedType);

        // Then: 应该返回 STRING 类型
        assertEquals(FieldType.Primitive.STRING, result, "未指定类型时应该返回 STRING");
    }

    @Test
    void shouldParseSpecificTypesForA4HeadRowWhenValidTypeIsProvided() {
        // Given: A4 HeadRow 和有效的类型字符串
        HeadRow a4HeadRow = HeadRows.A4;

        // When & Then: 验证各种类型的解析
        assertEquals(FieldType.Primitive.INT, a4HeadRow.parseType("INT"), "INT 应该解析为 INT 类型");
        assertEquals(FieldType.Primitive.INT, a4HeadRow.parseType("SHORT"), "SHORT 应该解析为 INT 类型");
        assertEquals(FieldType.Primitive.INT, a4HeadRow.parseType("BYTE"), "BYTE 应该解析为 INT 类型");
        assertEquals(FieldType.Primitive.LONG, a4HeadRow.parseType("LONG"), "LONG 应该解析为 LONG 类型");
        assertEquals(FieldType.Primitive.LONG, a4HeadRow.parseType("INT64"), "INT64 应该解析为 LONG 类型");
        assertEquals(FieldType.Primitive.FLOAT, a4HeadRow.parseType("FLOAT"), "FLOAT 应该解析为 FLOAT 类型");
        assertEquals(FieldType.Primitive.BOOL, a4HeadRow.parseType("BOOL"), "BOOL 应该解析为 BOOL 类型");
        assertEquals(FieldType.Primitive.STRING, a4HeadRow.parseType("STRING"), "STRING 应该解析为 STRING 类型");
        assertEquals(FieldType.Primitive.STRING, a4HeadRow.parseType("SLICEBYTE"), "SLICEBYTE 应该解析为 STRING 类型");
        assertEquals(FieldType.Primitive.STRING, a4HeadRow.parseType("HASHID"), "HASHID 应该解析为 STRING 类型");
    }

    @Test
    void shouldReturnNullForA4HeadRowWhenUnknownTypeIsProvided() {
        // Given: A4 HeadRow 和未知的类型字符串
        HeadRow a4HeadRow = HeadRows.A4;
        String unknownType = "UNKNOWN_TYPE";

        // When: 解析未知类型
        FieldType result = a4HeadRow.parseType(unknownType);

        // Then: 应该返回 null
        assertNull(result, "未知类型应该返回 null");
    }

    @Test
    void shouldParseValidLongValuesCorrectlyForAllHeadRowTypes() {
        // Given: 各种有效的长整数值
        String decimalValue = "123";
        String hexValue = "0x7B";
        String octalValue = "0173";
        String largeValue = "9223372036854775807";

        // When & Then: 验证所有 HeadRow 类型都能正确解析
        for (HeadRow headRow : new HeadRow[]{HeadRows.A2_Default, HeadRows.A3, HeadRows.A4}) {
            assertEquals(123L, headRow.parseLong(decimalValue), "应该正确解析十进制值");
            assertEquals(123L, headRow.parseLong(hexValue), "应该正确解析十六进制值");
            assertEquals(123L, headRow.parseLong(octalValue), "应该正确解析八进制值");
            assertEquals(9223372036854775807L, headRow.parseLong(largeValue), "应该正确解析大数值");
        }
    }

    @Test
    void shouldParseLongValuesWithAsteriskPrefixCorrectly() {
        // Given: 以 * 开头的长整数值（避免 Excel 科学计数法问题）
        String valueWithAsterisk = "*123456789012345";

        // When & Then: 验证所有 HeadRow 类型都能正确解析
        for (HeadRow headRow : new HeadRow[]{HeadRows.A2_Default, HeadRows.A3, HeadRows.A4}) {
            assertEquals(123456789012345L, headRow.parseLong(valueWithAsterisk),
                "应该正确解析带 * 前缀的值");
        }
    }

    @Test
    void shouldReturnZeroWhenParsingEmptyStringAsLong() {
        // Given: 空字符串
        String emptyValue = "";

        // When & Then: 验证所有 HeadRow 类型都能正确处理空值
        for (HeadRow headRow : new HeadRow[]{HeadRows.A2_Default, HeadRows.A3, HeadRows.A4}) {
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
    void shouldParseBooleanValuesCorrectlyForA4HeadRow() {
        // Given: A4 HeadRow
        HeadRow a4HeadRow = HeadRows.A4;

        // When & Then: 验证布尔值解析
        assertEquals(HeadRow.ParseBoolResult.TRUE, a4HeadRow.parseBool("1"), "'1' 应该解析为 TRUE");
        assertEquals(HeadRow.ParseBoolResult.TRUE, a4HeadRow.parseBool("true"), "'true' 应该解析为 TRUE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, a4HeadRow.parseBool("0"), "'0' 应该解析为 FALSE");
        assertEquals(HeadRow.ParseBoolResult.FALSE, a4HeadRow.parseBool("false"), "'false' 应该解析为 FALSE");
        assertEquals(HeadRow.ParseBoolResult.TRUE, a4HeadRow.parseBool("any_other_value"), "其他值应该解析为 TRUE");
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