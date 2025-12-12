package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelTableFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testReadWrite() throws Exception {
        Path excelPath = tempDir.resolve("test.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            row0.createCell(1).setCellValue("名字");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("1");
            row2.createCell(1).setCellValue("A");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ExcelTableFile tableFile = new ExcelTableFile(excelPath, "Sheet1", 2);

        // Empty row 2
        tableFile.emptyRows(2, 1, null);
        tableFile.saveAndClose();

        // Verify
        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");
            Row row2 = sheet.getRow(2);

            if (row2 != null) {
                // Check if cells are blank
                Cell c0 = row2.getCell(0);
                if (c0 != null) {
                    assertEquals(CellType.BLANK, c0.getCellType());
                }
                Cell c1 = row2.getCell(1);
                if (c1 != null) {
                    assertEquals(CellType.BLANK, c1.getCellType());
                }
            }
        }
    }

    @Test
    void testInsertRecordBlock() throws Exception {
        Path excelPath = tempDir.resolve("insert.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            row0.createCell(1).setCellValue("名字");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("1");
            row2.createCell(1).setCellValue("A");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ExcelTableFile tableFile = new ExcelTableFile(excelPath, "Sheet1", 2);

        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        tableFile.insertRecordBlock(-1, 0, transformed);
        tableFile.saveAndClose();

        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");
            Row row3 = sheet.getRow(3);
            assertNotNull(row3);
            assertEquals("2", row3.getCell(0).getStringCellValue());
            assertEquals("B", row3.getCell(1).getStringCellValue());
        }
    }

    @Test
    void testInsertWithShift() throws Exception {
        Path excelPath = tempDir.resolve("shift.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            row0.createCell(1).setCellValue("名字");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("1");
            row2.createCell(1).setCellValue("A");

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("3");
            row3.createCell(1).setCellValue("C");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ExcelTableFile tableFile = new ExcelTableFile(excelPath, "Sheet1", 2);

        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at row 3 (between 1,A and 3,C)
        tableFile.insertRecordBlock(3, 0, transformed);
        tableFile.saveAndClose();

        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");

            // Row 2: 1, A
            Row row2 = sheet.getRow(2);
            assertEquals("1", row2.getCell(0).getStringCellValue());
            assertEquals("A", row2.getCell(1).getStringCellValue());

            // Row 3: 2, B (Inserted)
            Row row3 = sheet.getRow(3);
            assertEquals("2", row3.getCell(0).getStringCellValue());
            assertEquals("B", row3.getCell(1).getStringCellValue());

            // Row 4: 3, C (Shifted)
            Row row4 = sheet.getRow(4);
            assertEquals("3", row4.getCell(0).getStringCellValue());
            assertEquals("C", row4.getCell(1).getStringCellValue());
        }
    }

    @Test
    void testInsertMultipleRowsWithShift() throws Exception {
        Path excelPath = tempDir.resolve("shift_multi.xlsx");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            row0.createCell(1).setCellValue("名字");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("1");
            row2.createCell(1).setCellValue("A");

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("4");
            row3.createCell(1).setCellValue("D");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ExcelTableFile tableFile = new ExcelTableFile(excelPath, "Sheet1", 2);

        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at row 3 (between 1,A and 4,D)
        tableFile.insertRecordBlock(3, 0, transformed);
        tableFile.saveAndClose();

        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");

            // Row 2: 1, A
            Row row2 = sheet.getRow(2);
            assertEquals("1", row2.getCell(0).getStringCellValue());
            assertEquals("A", row2.getCell(1).getStringCellValue());

            // Row 3: 2, B (Inserted)
            Row row3 = sheet.getRow(3);
            assertEquals("2", row3.getCell(0).getStringCellValue());
            assertEquals("B", row3.getCell(1).getStringCellValue());

            // Row 4: 3, C (Inserted)
            Row row4 = sheet.getRow(4);
            assertEquals("3", row4.getCell(0).getStringCellValue());
            assertEquals("C", row4.getCell(1).getStringCellValue());

            // Row 5: 4, D (Shifted)
            Row row5 = sheet.getRow(5);
            assertEquals("4", row5.getCell(0).getStringCellValue());
            assertEquals("D", row5.getCell(1).getStringCellValue());
        }
    }
}
