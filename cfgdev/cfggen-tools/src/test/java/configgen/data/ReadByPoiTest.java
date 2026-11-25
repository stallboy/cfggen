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

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReadByPoiTest {
    @TempDir Path root;

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
}
