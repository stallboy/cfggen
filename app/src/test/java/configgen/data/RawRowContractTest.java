package configgen.data;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RawRowContractTest {
    @TempDir Path root;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void fakeMatchesRealReadersForWhitespaceAndMissingCells(boolean excel) throws Exception {
        String[][] cells = {{" id ", " name ", " note "},
                {"id", "name", "note"}, {" 1 ", " Alice ", " "}, {"2"}};
        Path file = root.resolve(excel ? "t.xlsx" : "t.csv");
        if (excel) {
            try (var workbook = new XSSFWorkbook()) {
                var sheet = workbook.createSheet("t");
                for (int r = 0; r < cells.length; r++) {
                    var row = sheet.createRow(r);
                    for (int c = 0; c < cells[r].length; c++) row.createCell(c).setCellValue(cells[r][c]);
                }
                try (var out = Files.newOutputStream(file)) { workbook.write(out); }
            }
        } else {
            Files.writeString(file, " id , name , note \nid,name,note\n 1 , Alice , \n2\n");
        }
        ReadResult result = excel
                ? ReadByFastExcel.INSTANCE.readExcels(file, file.getFileName(), null)
                : new ReadCsv("UTF-8").readCsv(file, file.getFileName(), "t", 0);
        var sheet = result.sheets().getFirst().sheet();
        assertEquals(cells.length, sheet.rows().size());
        for (int r = 0; r < cells.length; r++) {
            var fake = new FakeRows.FakeRow(cells[r]);
            var actual = sheet.rows().get(r);
            assertEquals(fake.count(), actual.count());
            for (int c = 0; c < 4; c++) assertEquals(fake.cell(c), actual.cell(c), "row=" + r + ", col=" + c);
        }
        var table = CfgData.DTable.of("t", new ArrayList<>(List.of(sheet)));
        var stat = new CfgDataStat();
        HeadParser.parse(table, stat);
        CellParser.parse(table, stat, 2);
        assertEquals(List.of(List.of("1", "Alice", ""), List.of("2", "", "")),
                table.rows().stream().map(row -> row.stream().map(CfgData.DCell::value).toList()).toList());
    }

    @Test
    void fastExcelReturnsAnEmptyTableForAnEmptySheet() throws Exception {
        Path file = root.resolve("empty.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            workbook.createSheet("empty");
            try (var out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }

        var reader = new CfgDataReader(HeadRows.A2_Default, new ReadCsv("UTF-8"), ReadByFastExcel.INSTANCE);
        var data = reader.readCfgData(List.of(new ExcelFileInfo(0, file, file.getFileName(),
                DataUtil.FileFmt.EXCEL)), null, configgen.schema.CfgSchemaErrs.of());
        var table = data.tables().get("empty");

        assertNotNull(table);
        assertTrue(table.rows().isEmpty());
        assertEquals(1, data.stat().emptyTableCount());
    }

    @Test
    void poiPreservesPhysicalRowNumbersAndEvaluatesFormulas() throws Exception {
        Path file = root.resolve("calculated.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("calculated");
            sheet.createRow(0).createCell(0).setCellValue("result");
            sheet.createRow(1).createCell(0).setCellValue("result");
            sheet.createRow(4).createCell(0).setCellFormula("1+2");
            try (var out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }

        var read = ReadByPoi.INSTANCE.readExcels(file, file.getFileName(), null);
        var table = CfgData.DTable.of("calculated", new ArrayList<>(List.of(read.sheets().getFirst().sheet())));
        HeadParser.parse(table, read.stat());
        CellParser.parse(table, read.stat(), 2);

        assertEquals(1, table.rows().size());
        var value = table.rows().getFirst().getFirst();
        assertEquals("3", value.value());
        assertEquals(5, value.displayRow());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void poiPreservesSparseColumnsAndFormulaResults(boolean xlsx) throws Exception {
        Path file = root.resolve(xlsx ? "sparse.xlsx" : "sparse.xls");
        int lastColumn = xlsx ? 16383 : 255;
        try (var workbook = xlsx ? new XSSFWorkbook() : new HSSFWorkbook()) {
            var row = workbook.createSheet("sparse").createRow(0);
            row.createCell(lastColumn).setCellFormula("A1+2");
            row.createCell(0).setCellValue(1);
            row.createCell(2);
            try (var out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }

        var read = ReadByPoi.INSTANCE.readExcels(file, file.getFileName(), null);
        var row = read.sheets().getFirst().sheet().rows().getFirst();
        assertEquals(lastColumn + 1, row.count());
        assertEquals("1", row.cell(0));
        assertEquals("", row.cell(1));
        assertEquals("", row.cell(2));
        assertEquals("", row.cell(lastColumn - 1));
        assertEquals("3", row.cell(lastColumn));
        assertEquals("", row.cell(lastColumn + 1));
        assertEquals(1, read.stat().cellFormulaCount);
    }

    @Test
    void fastExcelReadsTheCachedValueOfAFormulaCell() throws Exception {
        Path file = root.resolve("calculated.xlsx");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("calculated");
            sheet.createRow(0).createCell(0).setCellValue("result");
            sheet.createRow(1).createCell(0).setCellValue("result");
            var formula = sheet.createRow(2).createCell(0);
            formula.setCellFormula("\"calculated value\"");
            workbook.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(formula);
            try (var out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
        var read = ReadByFastExcel.INSTANCE.readExcels(file, file.getFileName(), null);
        var table = CfgData.DTable.of("calculated", new ArrayList<>(List.of(read.sheets().getFirst().sheet())));

        HeadParser.parse(table, read.stat());
        CellParser.parse(table, read.stat(), 2);

        assertEquals(List.of("calculated value"),
                table.rows().stream().map(row -> row.getFirst().value()).toList());
    }

}
