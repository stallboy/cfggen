package configgen.write;

import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ExcelTableFile extends AbstractExcelTableFile {
    public ExcelTableFile(@NotNull Path filePath,
                          @NotNull String sheetName,
                          int headRow) {
        super(filePath, sheetName, headRow);
    }

    @Override
    protected int lastLineNum() {
        return sheet.getLastRowNum();
    }

    @Override
    protected void blankCell(int line, int index) {
        Row row = sheet.getRow(line);
        if (row != null) {
            Cell cell = row.getCell(index);
            if (cell != null) {
                cell.setBlank();
            }
        }
    }

    @Override
    protected void blankLine(int line) {
        Row row = sheet.getRow(line);
        if (row != null) {
            for (Cell cell : row) {
                cell.setBlank();
            }
        }
    }

    @Override
    protected int appendLineNum() {
        return sheet.getLastRowNum() + 1;
    }

    @Override
    protected void shiftLines(int from, int count) {
        // 将指定行及后续行向下移动指定行数
        int lastRowNum = sheet.getLastRowNum();
        if (from > lastRowNum) {
            return;
        }
        sheet.shiftRows(from, lastRowNum, count);
    }

    @Override
    protected void writeLine(int lineNum, String[] lineData) {
        Row row = sheet.getRow(lineNum);
        if (row == null) {
            row = sheet.createRow(lineNum);
        }

        if (lineData != null) {
            // 写入该行的所有单元格
            for (int col = 0; col < lineData.length; col++) {
                String cellValue = lineData[col];
                if (cellValue != null) { // null时，前置步骤保证了cell是blank
                    Cell cell = row.getCell(col);
                    if (cell == null) {
                        cell = row.createCell(col);
                    }
                    cell.setCellValue(cellValue);
                }
            }
        }
    }

}
