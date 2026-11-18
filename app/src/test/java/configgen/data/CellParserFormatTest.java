package configgen.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.FakeRows.FakeRow;
import static org.junit.jupiter.api.Assertions.*;

class CellParserFormatTest {

    @Test
    void shouldParseMixedDataTypesInRowMode() {
        // Given: 混合数据类型行
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("mixed.csv", "mixed", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "name", "age", "price", "active"}),
                    new FakeRow(new String[]{"id", "name", "age", "price", "active"}),
                    new FakeRow(new String[]{"1", "Alice", "25", "99.99", "true"}),
                    new FakeRow(new String[]{"2", "Bob", "30", "149.50", "false"}),
                    new FakeRow(new String[]{"3", "Charlie", "35", "", "true"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("mixed", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 行模式解析
        HeadParser.parse(dt, ds);
        CellParser.parse(dt, ds, 2);

        // Then: 验证数据类型正确识别
        assertEquals(3, dt.rows().size());

        // 验证第一行数据
        List<CfgData.DCell> row1 = dt.rows().get(0);
        assertEquals("1", row1.get(0).value());
        assertEquals("Alice", row1.get(1).value());
        assertEquals("25", row1.get(2).value());
        assertEquals("99.99", row1.get(3).value());
        assertEquals("true", row1.get(4).value());

        // 验证第二行数据
        List<CfgData.DCell> row2 = dt.rows().get(1);
        assertEquals("2", row2.get(0).value());
        assertEquals("Bob", row2.get(1).value());
        assertEquals("30", row2.get(2).value());
        assertEquals("149.50", row2.get(3).value());
        assertEquals("false", row2.get(4).value());

        // 验证第三行数据（包含空值）
        List<CfgData.DCell> row3 = dt.rows().get(2);
        assertEquals("3", row3.get(0).value());
        assertEquals("Charlie", row3.get(1).value());
        assertEquals("35", row3.get(2).value());
        assertEquals("", row3.get(3).value());
        assertEquals("true", row3.get(4).value());

        assertEquals(3, ds.rowCount);
        assertEquals(0, ds.ignoredRowCount);
    }

    @Test
    void shouldHandleLargeExcelFilesEfficiently() {
        // Given: 模拟大数据集
        List<FakeRow> largeDataRows = new ArrayList<>();

        // 添加表头
        largeDataRows.add(new FakeRow(new String[]{"id", "name", "value"}));
        largeDataRows.add(new FakeRow(new String[]{"id", "name", "value"}));

        // 添加大量数据行（模拟大文件）
        for (int i = 0; i < 100; i++) {
            largeDataRows.add(new FakeRow(new String[]{
                String.valueOf(i + 1),
                "User" + (i + 1),
                "Value" + (i + 1)
            }));
        }

        CfgData.DRawSheet sheet = new CfgData.DRawSheet("large.csv", "large", 0,
                new ArrayList<>(largeDataRows), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("large", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析单元格
        long startTime = System.currentTimeMillis();
        HeadParser.parse(dt, ds);
        CellParser.parse(dt, ds, 2);
        long endTime = System.currentTimeMillis();

        // Then: 验证性能可接受
        long processingTime = endTime - startTime;
        assertTrue(processingTime < 1000, "Processing should complete within 1 second for 100 rows");

        // 验证数据完整性
        assertEquals(100, dt.rows().size());
        assertEquals(100, ds.rowCount);

        // 验证随机样本数据
        List<CfgData.DCell> firstRow = dt.rows().get(0);
        assertEquals("1", firstRow.get(0).value());
        assertEquals("User1", firstRow.get(1).value());
        assertEquals("Value1", firstRow.get(2).value());

        List<CfgData.DCell> lastRow = dt.rows().get(99);
        assertEquals("100", lastRow.get(0).value());
        assertEquals("User100", lastRow.get(1).value());
        assertEquals("Value100", lastRow.get(2).value());
    }

    @Test
    void shouldSkipCommentsAndEmptyRowsCorrectly() {
        // Given: 包含注释和空行的数据
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("comments.csv", "comments", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "name", "value"}),
                    new FakeRow(new String[]{"id", "name", "value"}),
                    new FakeRow(new String[]{"# 这是注释行", "", ""}),
                    new FakeRow(new String[]{"1", "Alice", "100"}),
                    new FakeRow(new String[]{"", "", ""}), // 空行
                    new FakeRow(new String[]{"2", "Bob", "200"}),
                    new FakeRow(new String[]{"# 另一个注释", "", ""}),
                    new FakeRow(new String[]{"", "", ""}), // 全空行
                    new FakeRow(new String[]{"3", "Charlie", "300"})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("comments", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析单元格
        HeadParser.parse(dt, ds);
        CellParser.parse(dt, ds, 2);

        // Then: 验证正确跳过注释和空行
        assertEquals(3, dt.rows().size());
        assertEquals(3, ds.rowCount);
        assertEquals(4, ds.ignoredRowCount); // 2个注释行 + 2个空行

        // 验证有效数据
        List<CfgData.DCell> row1 = dt.rows().get(0);
        assertEquals("1", row1.get(0).value());
        assertEquals("Alice", row1.get(1).value());
        assertEquals("100", row1.get(2).value());

        List<CfgData.DCell> row2 = dt.rows().get(1);
        assertEquals("2", row2.get(0).value());
        assertEquals("Bob", row2.get(1).value());
        assertEquals("200", row2.get(2).value());

        List<CfgData.DCell> row3 = dt.rows().get(2);
        assertEquals("3", row3.get(0).value());
        assertEquals("Charlie", row3.get(1).value());
        assertEquals("300", row3.get(2).value());
    }

    @Test
    void shouldPreserveQuotedCsvCells(@TempDir Path root) throws Exception {
        Path file = root.resolve("special.csv");
        Files.writeString(file, """
                id,description,path
                id,description,path
                1,"say ""hello"", then leave",C:\\Users\\test
                2,Price < $100,/home/user/data
                """);
        var read = new ReadCsv("UTF-8").readCsv(file, file.getFileName(), "special", 0, ',', null);
        var table = CfgData.DTable.of("special", new ArrayList<>(List.of(read.sheets().getFirst().sheet())));

        HeadParser.parse(table, read.stat());
        CellParser.parse(table, read.stat(), 2);

        assertEquals(List.of(List.of("1", "say \"hello\", then leave", "C:\\Users\\test"),
                        List.of("2", "Price < $100", "/home/user/data")),
                table.rows().stream().map(row -> row.stream().map(CfgData.DCell::value).toList()).toList());
    }

    @Test
    void shouldParseDataWithMissingValues() {
        // Given: 包含缺失值的数据
        CfgData.DRawSheet sheet = new CfgData.DRawSheet("missing.csv", "missing", 0,
                new ArrayList<>(List.of(
                    new FakeRow(new String[]{"id", "name", "age", "city"}),
                    new FakeRow(new String[]{"id", "name", "age", "city"}),
                    new FakeRow(new String[]{"1", "Alice", "25", ""}),
                    new FakeRow(new String[]{"2", "", "30", "New York"}),
                    new FakeRow(new String[]{"3", "Bob", "", "London"}),
                    new FakeRow(new String[]{"4", "", "", ""})
                )), new ArrayList<>());
        CfgData.DTable dt = CfgData.DTable.of("missing", new ArrayList<>(List.of(sheet)));
        CfgDataStat ds = new CfgDataStat();

        // When: 解析单元格
        HeadParser.parse(dt, ds);
        CellParser.parse(dt, ds, 2);

        // Then: 验证缺失值正确处理
        assertEquals(4, dt.rows().size());

        // 验证第一行（城市缺失）
        List<CfgData.DCell> row1 = dt.rows().get(0);
        assertEquals("1", row1.get(0).value());
        assertEquals("Alice", row1.get(1).value());
        assertEquals("25", row1.get(2).value());
        assertEquals("", row1.get(3).value());

        // 验证第二行（姓名缺失）
        List<CfgData.DCell> row2 = dt.rows().get(1);
        assertEquals("2", row2.get(0).value());
        assertEquals("", row2.get(1).value());
        assertEquals("30", row2.get(2).value());
        assertEquals("New York", row2.get(3).value());

        // 验证第三行（年龄缺失）
        List<CfgData.DCell> row3 = dt.rows().get(2);
        assertEquals("3", row3.get(0).value());
        assertEquals("Bob", row3.get(1).value());
        assertEquals("", row3.get(2).value());
        assertEquals("London", row3.get(3).value());

        // 验证第四行（所有值缺失）
        List<CfgData.DCell> row4 = dt.rows().get(3);
        assertEquals("4", row4.get(0).value());
        assertEquals("", row4.get(1).value());
        assertEquals("", row4.get(2).value());
        assertEquals("", row4.get(3).value());
    }
}