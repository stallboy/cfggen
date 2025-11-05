package configgen.data;

import configgen.schema.CfgSchemaErrs;
import configgen.schema.CfgSchemaException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.FakeRows.FakeRow;
import static org.junit.jupiter.api.Assertions.*;

class HeadParserBoundaryTest {

    @Test
    void shouldParseHeadersWithSpecialCharacters() {
        // Given: 包含特殊字符的表头
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("special.csv", "special", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "name@domain", "price$", "count#number", "desc[test]"}),
                    new FakeRow(new String[]{"id", "name", "price", "count", "desc"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("special", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析表头
        HeadParser.parse(dt, ds);

        // Then: 验证字段名称正确处理
        assertEquals(5, dt.fields().size());

        CfgData.DField idField = dt.fields().get(0);
        assertEquals("id", idField.name());
        assertEquals("", idField.comment());

        CfgData.DField nameField = dt.fields().get(1);
        assertEquals("name", nameField.name());
        assertEquals("name@domain", nameField.comment());

        CfgData.DField priceField = dt.fields().get(2);
        assertEquals("price", priceField.name());
        assertEquals("price$", priceField.comment());

        CfgData.DField countField = dt.fields().get(3);
        assertEquals("count", countField.name());
        assertEquals("count#number", countField.comment());

        CfgData.DField descField = dt.fields().get(4);
        assertEquals("desc", descField.name());
        assertEquals("desc[test]", descField.comment());

        assertEquals(List.of(0, 1, 2, 3, 4), sheet.fieldIndices());
        assertEquals(5, ds.columnCount);
        assertEquals(0, ds.ignoredColumnCount);
    }

    @Test
    void shouldHandleEmptyHeaderRow() {
        // Given: 空表头行
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("empty.csv", "empty", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"", "", ""}),
                    new FakeRow(new String[]{"id", "name", "value"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("empty", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析表头
        HeadParser.parse(dt, ds);

        // Then: 返回空字段列表
        assertEquals(3, dt.fields().size());

        CfgData.DField idField = dt.fields().get(0);
        assertEquals("id", idField.name());
        assertEquals("", idField.comment());

        CfgData.DField nameField = dt.fields().get(1);
        assertEquals("name", nameField.name());
        assertEquals("", nameField.comment());

        CfgData.DField valueField = dt.fields().get(2);
        assertEquals("value", valueField.name());
        assertEquals("", valueField.comment());

        assertEquals(List.of(0, 1, 2), sheet.fieldIndices());
        assertEquals(3, ds.columnCount);
        assertEquals(0, ds.ignoredColumnCount);
    }

    @Test
    void shouldDetectInconsistentHeadersAcrossSheets() {
        // Given: 不一致的多表头
        CfgData.DRawSheet sheet1 = new CfgData.DRawSheet("multi.csv", "sheet1", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "name", "value"}),
                    new FakeRow(new String[]{"id", "name", "value"})
                )), new ArrayList<>());

        CfgData.DRawSheet sheet2 = new CfgData.DRawSheet("multi.csv", "sheet2", 1,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "description", "amount"}),
                    new FakeRow(new String[]{"id", "description", "amount"})
                )), new ArrayList<>());

        CfgData.DTable dt = CfgData.DTable.of("multi", new ArrayList<>(List.of(sheet1, sheet2)));
        CfgDataStat ds = new CfgDataStat();

        // When & Then: 解析表头应该抛出异常
        CfgSchemaException exception = assertThrows(CfgSchemaException.class, () -> {
            HeadParser.parse(dt, ds);
        });

        // 验证异常消息包含正确的信息
        CfgSchemaErrs errs = exception.getErrs();
        assertEquals(1, errs.errs().size());
        CfgSchemaErrs.Err err = errs.errs().getFirst();
        assertInstanceOf(CfgSchemaErrs.SplitDataHeaderNotEqual.class, err);
        CfgSchemaErrs.SplitDataHeaderNotEqual splitErr = (CfgSchemaErrs.SplitDataHeaderNotEqual) err;
        // Debug: print actual values
        System.out.println("sheet1: " + splitErr.sheet1());
        System.out.println("sheet2: " + splitErr.sheet2());
        assertEquals("multi.csv[sheet2]", splitErr.sheet1());
        assertEquals("multi.csv[sheet1]", splitErr.sheet2());
    }

    @Test
    void shouldHandleHeadersWithOnlyWhitespace() {
        // Given: 只包含空格的表头
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("whitespace.csv", "whitespace", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"  ", "\t", "   \t   "}),
                    new FakeRow(new String[]{"id", "name", "value"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("whitespace", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析表头
        HeadParser.parse(dt, ds);

        // Then: 验证空白表头被正确处理
        assertEquals(3, dt.fields().size());

        CfgData.DField idField = dt.fields().get(0);
        assertEquals("id", idField.name());
        assertEquals("", idField.comment());

        CfgData.DField nameField = dt.fields().get(1);
        assertEquals("name", nameField.name());
        assertEquals("", nameField.comment());

        CfgData.DField valueField = dt.fields().get(2);
        assertEquals("value", valueField.name());
        assertEquals("", valueField.comment());

        assertEquals(List.of(0, 1, 2), sheet.fieldIndices());
        assertEquals(3, ds.columnCount);
        assertEquals(0, ds.ignoredColumnCount);
    }

    @Test
    void shouldParseHeadersWithMixedLanguages() {
        // Given: 混合语言的表头
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("mixed.csv", "mixed", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "名称", "説明", "description"}),
                    new FakeRow(new String[]{"id", "name", "note", "desc"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("mixed", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析表头
        HeadParser.parse(dt, ds);

        // Then: 验证多语言表头正确处理
        assertEquals(4, dt.fields().size());

        CfgData.DField idField = dt.fields().get(0);
        assertEquals("id", idField.name());
        assertEquals("", idField.comment());

        CfgData.DField nameField = dt.fields().get(1);
        assertEquals("name", nameField.name());
        assertEquals("名称", nameField.comment());

        CfgData.DField noteField = dt.fields().get(2);
        assertEquals("note", noteField.name());
        assertEquals("説明", noteField.comment());

        CfgData.DField descField = dt.fields().get(3);
        assertEquals("desc", descField.name());
        assertEquals("description", descField.comment());

        assertEquals(List.of(0, 1, 2, 3), sheet.fieldIndices());
        assertEquals(4, ds.columnCount);
        assertEquals(0, ds.ignoredColumnCount);
    }

    @Test
    void shouldHandleHeadersWithCommaInName() {
        // Given: 包含逗号的表头名称
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("comma.csv", "comma", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "name,alias", "value,type"}),
                    new FakeRow(new String[]{"id", "name", "value"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("comma", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析表头
        HeadParser.parse(dt, ds);

        // Then: 验证逗号分隔的名称正确处理
        assertEquals(3, dt.fields().size());

        CfgData.DField idField = dt.fields().get(0);
        assertEquals("id", idField.name());
        assertEquals("", idField.comment());

        CfgData.DField nameField = dt.fields().get(1);
        assertEquals("name", nameField.name());
        assertEquals("name,alias", nameField.comment());

        CfgData.DField valueField = dt.fields().get(2);
        assertEquals("value", valueField.name());
        assertEquals("value,type", valueField.comment());

        assertEquals(List.of(0, 1, 2), sheet.fieldIndices());
        assertEquals(3, ds.columnCount);
        assertEquals(0, ds.ignoredColumnCount);
    }
}