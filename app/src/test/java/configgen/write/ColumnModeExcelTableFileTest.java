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

class ColumnModeExcelTableFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testReadWriteColumnMode() throws Exception {
        Path excelPath = tempDir.resolve("test_column.xlsx");
        // 创建列模式的Excel文件
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            // 列0: ["ID", "名字"]
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("名字");

            // 列1: ["id", "name"]
            row0.createCell(1).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            // 列2: ["1", "A"]
            row0.createCell(2).setCellValue("1");
            row1.createCell(2).setCellValue("A");

            // 列3: ["2", "B"]
            row0.createCell(3).setCellValue("2");
            row1.createCell(3).setCellValue("B");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ColumnModeExcelTableFile tableFile = new ColumnModeExcelTableFile(excelPath, "Sheet1", 2);

        // Test emptyRows - 在列模式下，emptyRows实际上是清空列
        // 清空第2列（从0开始），即清空数据列"1,A"
        tableFile.emptyRows(2, 1, null);
        tableFile.saveAndClose();

        // Verify
        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");

            // 检查第2列是否被清空
            Row row0 = sheet.getRow(0);
            Row row1 = sheet.getRow(1);

            // 第2列应该为空
            Cell cell20 = row0.getCell(2);
            if (cell20 != null) {
                assertEquals(CellType.BLANK, cell20.getCellType());
            }
            Cell cell21 = row1.getCell(2);
            if (cell21 != null) {
                assertEquals(CellType.BLANK, cell21.getCellType());
            }

            // 其他列应该保持不变
            assertEquals("ID", row0.getCell(0).getStringCellValue());
            assertEquals("id", row0.getCell(1).getStringCellValue());
            assertEquals("2", row0.getCell(3).getStringCellValue());

            assertEquals("名字", row1.getCell(0).getStringCellValue());
            assertEquals("name", row1.getCell(1).getStringCellValue());
            assertEquals("B", row1.getCell(3).getStringCellValue());
        }
    }

    @Test
    void testInsertRecordBlockColumnMode() throws Exception {
        Path excelPath = tempDir.resolve("insert_column.xlsx");
        // 创建初始Excel文件
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            // 列0: ["ID", "名字"]
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("名字");

            // 列1: ["id", "name"]
            row0.createCell(1).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            // 列2: ["1", "A"]
            row0.createCell(2).setCellValue("1");
            row1.createCell(2).setCellValue("A");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ColumnModeExcelTableFile tableFile = new ColumnModeExcelTableFile(excelPath, "Sheet1", 2);

        // 创建RecordBlock（逻辑层按行存储）
        // 要插入的数据：
        // 行0: ["2", "B"]
        // 行1: ["3", "C"]
        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");

        // Identity mapping
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at end (列模式：插入到最后一列)
        tableFile.insertRecordBlock(-1, 0, transformed);
        tableFile.saveAndClose();

        // Verify
        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");

            // 检查原始数据
            Row row0 = sheet.getRow(0);
            Row row1 = sheet.getRow(1);

            assertEquals("ID", row0.getCell(0).getStringCellValue());
            assertEquals("id", row0.getCell(1).getStringCellValue());
            assertEquals("1", row0.getCell(2).getStringCellValue());
            assertEquals("2", row0.getCell(3).getStringCellValue());
            assertEquals("3", row0.getCell(4).getStringCellValue());

            assertEquals("名字", row1.getCell(0).getStringCellValue());
            assertEquals("name", row1.getCell(1).getStringCellValue());
            assertEquals("A", row1.getCell(2).getStringCellValue());
            assertEquals("B", row1.getCell(3).getStringCellValue());
            assertEquals("C", row1.getCell(4).getStringCellValue());
        }
    }

    @Test
    void testInsertWithShiftColumnMode() throws Exception {
        Path excelPath = tempDir.resolve("shift_column.xlsx");
        // 创建初始Excel文件
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            // 列0: ["ID", "名字"]
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("名字");

            // 列1: ["id", "name"]
            row0.createCell(1).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            // 列2: ["1", "A"]
            row0.createCell(2).setCellValue("1");
            row1.createCell(2).setCellValue("A");

            // 列3: ["3", "C"]
            row0.createCell(3).setCellValue("3");
            row1.createCell(3).setCellValue("C");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ColumnModeExcelTableFile tableFile = new ColumnModeExcelTableFile(excelPath, "Sheet1", 2);

        // 要插入的数据：
        // 逻辑层：行0: ["2", "B"]
        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at column 2 (在列2的位置插入，即"1,A"列之前)
        tableFile.insertRecordBlock(2, 0, transformed);
        tableFile.saveAndClose();

        // Verify
        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");

            Row row0 = sheet.getRow(0);
            Row row1 = sheet.getRow(1);

            // 验证插入和移动后的结果
            assertEquals("ID", row0.getCell(0).getStringCellValue());
            assertEquals("id", row0.getCell(1).getStringCellValue());
            assertEquals("2", row0.getCell(2).getStringCellValue());  // 插入的数据
            assertEquals("1", row0.getCell(3).getStringCellValue());  // 原始数据被移动
            assertEquals("3", row0.getCell(4).getStringCellValue());  // 原始数据被移动

            assertEquals("名字", row1.getCell(0).getStringCellValue());
            assertEquals("name", row1.getCell(1).getStringCellValue());
            assertEquals("B", row1.getCell(2).getStringCellValue());  // 插入的数据
            assertEquals("A", row1.getCell(3).getStringCellValue());  // 原始数据被移动
            assertEquals("C", row1.getCell(4).getStringCellValue());  // 原始数据被移动
        }
    }

    @Test
    void testInsertMultipleColumnsColumnMode() throws Exception {
        Path excelPath = tempDir.resolve("multi_column.xlsx");
        // 创建初始Excel文件
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            // 列0: ["ID", "名字"]
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("ID");
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("名字");

            // 列1: ["id", "name"]
            row0.createCell(1).setCellValue("id");
            row1.createCell(1).setCellValue("name");

            // 列2: ["1", "A"]
            row0.createCell(2).setCellValue("1");
            row1.createCell(2).setCellValue("A");

            // 列3: ["4", "D"]
            row0.createCell(3).setCellValue("4");
            row1.createCell(3).setCellValue("D");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }

        ColumnModeExcelTableFile tableFile = new ColumnModeExcelTableFile(excelPath, "Sheet1", 2);

        // 要插入的多列数据：
        // 逻辑层：
        // 行0: ["2", "B"]
        // 行1: ["3", "C"]
        RecordBlock block = new RecordBlock(2);
        block.setCell(0, 0, "2");
        block.setCell(0, 1, "B");
        block.setCell(1, 0, "3");
        block.setCell(1, 1, "C");
        RecordBlockTransformed transformed = new RecordBlockTransformed(block, List.of(0, 1));

        // Insert at column 2 (在列2的位置插入两列)
        tableFile.insertRecordBlock(2, 0, transformed);
        tableFile.saveAndClose();

        // Verify
        try (Workbook wb = new XSSFWorkbook(excelPath.toFile())) {
            Sheet sheet = wb.getSheet("Sheet1");

            Row row0 = sheet.getRow(0);
            Row row1 = sheet.getRow(1);

            // 验证多列插入后的结果
            assertEquals("ID", row0.getCell(0).getStringCellValue());
            assertEquals("id", row0.getCell(1).getStringCellValue());
            assertEquals("2", row0.getCell(2).getStringCellValue());  // 插入的数据
            assertEquals("3", row0.getCell(3).getStringCellValue());  // 插入的数据
            assertEquals("1", row0.getCell(4).getStringCellValue());  // 原始数据被移动
            assertEquals("4", row0.getCell(5).getStringCellValue());  // 原始数据被移动

            assertEquals("名字", row1.getCell(0).getStringCellValue());
            assertEquals("name", row1.getCell(1).getStringCellValue());
            assertEquals("B", row1.getCell(2).getStringCellValue());  // 插入的数据
            assertEquals("C", row1.getCell(3).getStringCellValue());  // 插入的数据
            assertEquals("A", row1.getCell(4).getStringCellValue());  // 原始数据被移动
            assertEquals("D", row1.getCell(5).getStringCellValue());  // 原始数据被移动
        }
    }
}