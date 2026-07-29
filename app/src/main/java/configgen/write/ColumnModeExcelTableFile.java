package configgen.write;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ColumnModeExcelTableFile extends AbstractExcelTableFile {
    public ColumnModeExcelTableFile(@NotNull Path filePath,
                                    @NotNull String sheetName,
                                    int headRow) {
        super(filePath, sheetName, headRow);
    }

    @Override
    protected int lastLineNum() {
        return getColumnCount() - 1;
    }

    @Override
    protected void blankCell(int line, int index) {
        Row row = sheet.getRow(index);
        if (row != null) {
            Cell cell = row.getCell(line);
            if (cell != null) {
                cell.setBlank();
            }
        }
    }

    @Override
    protected void blankLine(int line) {
        for (Row row : sheet) {
            Cell cell = row.getCell(line);
            if (cell != null) {
                cell.setBlank();
            }
        }
    }

    @Override
    protected int appendLineNum() {
        return getColumnCount();
    }

    @Override
    protected void shiftLines(int from, int count) {
        // 将指定列及后续列向右移动指定列数
        int maxCols = getColumnCount();
        if (from >= maxCols) {
            return;
        }

        sheet.shiftColumns(from, maxCols - 1, count);
    }

    @Override
    protected void writeLine(int lineNum, String[] lineData) {
        if (lineData == null) {
            return;
        }

        // 写入该列的所有单元格
        for (int row = 0; row < lineData.length; row++) {
            String cellValue = lineData[row];
            if (cellValue != null) {
                Row sheetRow = sheet.getRow(row);
                if (sheetRow == null) {
                    sheetRow = sheet.createRow(row);
                }
                Cell cell = sheetRow.getCell(lineNum);
                if (cell == null) {
                    cell = sheetRow.createCell(lineNum);
                }
                cell.setCellValue(cellValue);
            }
        }
    }

    public int getColumnCount() {
        int maxCols = 0;
        for (Row row : sheet) {
            maxCols = Math.max(maxCols, row.getLastCellNum());
        }
        return maxCols;
    }

}
