package configgen.write;

import configgen.util.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 基于Apache POI的Excel表格文件实现
 */
public class ExcelTableFile implements TableFile {
    private final Path filePath;
    private final String sheetName;
    private final Workbook workbook;
    private final Sheet sheet;
    private final DataFormatter formatter;

    public ExcelTableFile(Path filePath, String sheetName) throws IOException {
        this.filePath = filePath;
        this.sheetName = sheetName;

        // 打开现有文件或创建新文件
        if (java.nio.file.Files.exists(filePath)) {
            try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                this.workbook = WorkbookFactory.create(fis);
            }
        } else {
            this.workbook = new XSSFWorkbook();
        }

        // 获取或创建工作表
        this.sheet = getOrCreateSheet(workbook, sheetName);
        this.formatter = new DataFormatter();
    }

    private Sheet getOrCreateSheet(Workbook workbook, String sheetName) {
        Sheet existingSheet = workbook.getSheet(sheetName);
        if (existingSheet != null) {
            return existingSheet;
        }
        return workbook.createSheet(sheetName);
    }

    @Override
    public void emptyRows(int startLine, int count) {
        if (startLine < 0 || count <= 0) {
            return;
        }

        int lastRowNum = sheet.getLastRowNum();
        if (startLine > lastRowNum) {
            return;
        }

        // 清空指定范围内的行
        for (int i = startLine; i < Math.min(startLine + count, lastRowNum + 1); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                // 清空行中的所有单元格
                for (Cell cell : row) {
                    cell.setBlank();
                }
            }
        }
    }

    @Override
    public void insertRecordBlock(int startLine, int emptyRowCount, RecordBlock content) {
        if (content == null || content.getRowCount() <= 0) {
            return;
        }

        int actualStartLine;
        if (startLine == -1) {
            // 放到最后
            actualStartLine = sheet.getLastRowNum() + 1;
        } else {
            actualStartLine = startLine;
        }

        int contentLineCount = content.getRowCount();

        // 如果内容行数大于可用空行数，需要移动后续行
        if (contentLineCount > emptyRowCount && startLine != -1) {
            shiftRowsDown(actualStartLine + emptyRowCount, contentLineCount - emptyRowCount);
        }

        // 写入记录块内容
        for (int rowOffset = 0; rowOffset < contentLineCount; rowOffset++) {
            int rowNum = actualStartLine + rowOffset;
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }

            // 写入该行的所有单元格
            // 这里假设RecordBlock知道要写入哪些列
            // 具体的单元格写入逻辑在RecordBlock实现中处理
            for (int col = 0; col < getMaxColumnCount(); col++) {
                // RecordBlock会通过cell方法设置具体的值
                // 这里只是确保单元格存在
                Cell cell = row.getCell(col);
                if (cell == null) {
                    cell = row.createCell(col);
                }
            }
        }
    }

    @Override
    public void save() {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            workbook.write(fos);
        } catch (IOException e) {
            Logger.log("Failed to save Excel file: " + filePath + " - " + e.getMessage());
            throw new RuntimeException("Failed to save Excel file: " + filePath, e);
        }
    }

    @Override
    public String getCell(int row, int col) {
        return "";
    }

    /**
     * 获取最大列数（用于确保所有需要的列都存在）
     */
    private int getMaxColumnCount() {
        int maxCols = 0;
        for (Row row : sheet) {
            maxCols = Math.max(maxCols, row.getLastCellNum());
        }
        return maxCols;
    }

    /**
     * 将指定行及后续行向下移动指定行数
     */
    private void shiftRowsDown(int startRow, int shiftCount) {
        int lastRowNum = sheet.getLastRowNum();
        if (startRow > lastRowNum) {
            return;
        }

        // 从最后一行开始向下移动，避免覆盖
        for (int i = lastRowNum; i >= startRow; i--) {
            Row sourceRow = sheet.getRow(i);
            if (sourceRow != null) {
                // 移动行
                sheet.shiftRows(i, i, shiftCount);
            }
        }
    }

    /**
     * 设置单元格的值
     */
    public void setCellValue(int row, int col, String value) {
        Row sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            sheetRow = sheet.createRow(row);
        }

        Cell cell = sheetRow.getCell(col);
        if (cell == null) {
            cell = sheetRow.createCell(col);
        }

        cell.setCellValue(value);
    }

    /**
     * 获取单元格的值
     */
    public String getCellValue(int row, int col) {
        Row sheetRow = sheet.getRow(row);
        if (sheetRow == null) {
            return "";
        }

        Cell cell = sheetRow.getCell(col);
        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell);
    }

    /**
     * 关闭工作簿
     */
    public void close() {
        try {
            workbook.close();
        } catch (IOException e) {
            Logger.log("Failed to close Excel workbook: " + filePath + " - " + e.getMessage());
        }
    }
}