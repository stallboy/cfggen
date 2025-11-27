package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ExcelTableFile extends AbstractExcelTableFile {
    public ExcelTableFile(@NotNull Path filePath,
                          @NotNull String sheetName,
                          int headRow) {
        super(filePath, sheetName, headRow);
    }


    /**
     * 清空指定行范围的数据
     * @param startRow 起始行号（从0开始）
     * @param count 要清空的行数
     */
    @Override
    public void emptyRows(int startRow, int count) {
        if (startRow < 0 || count <= 0) {
            return;
        }

        // 行模式：清空指定行范围的数据
        int lastRowNum = getDataLastRowNum();
        if (startRow > lastRowNum) {
            return;
        }

        // 清空指定范围内的行
        for (int i = startRow; i < Math.min(startRow + count, lastRowNum + 1); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                // 清空行中的所有单元格
                for (Cell cell : row) {
                    cell.setBlank();
                }
            }
        }

    }

    /**
     * 插入记录块到指定位置
     *
     * @param startRow 起始行号，-1表示放到最后
     * @param emptyRowCount 可用的空行数
     * @param content 记录块内容，包含要插入的数据
     */
    @Override
    public void insertRecordBlock(int startRow, int emptyRowCount, @NotNull RecordBlockTransformed content) {
        if (content.getRowCount() <= 0) {
            return;
        }

        // 行模式：插入记录块到指定行位置
        int actualStartRow;
        if (startRow == -1) {
            // 放到最后
            actualStartRow = Math.max(getDataLastRowNum() + 1, headRow);
        } else {
            actualStartRow = startRow;
        }

        int contentRowCount = content.getRowCount();

        // 如果内容行数大于可用空行数，需要移动后续行
        if (contentRowCount > emptyRowCount && startRow != -1) {
            shiftRowsDown(startRow + emptyRowCount, contentRowCount - emptyRowCount);
        }

        // 写入记录块内容
        for (int rowOffset = 0; rowOffset < contentRowCount; rowOffset++) {
            int rowNum = actualStartRow + rowOffset;
            Row row = sheet.getRow(rowNum);
            if (row == null) {
                row = sheet.createRow(rowNum);
            }

            // 获取RecordBlock中该行的数据
            String[] rowData = content.getRow(rowOffset);
            if (rowData != null) {
                // 写入该行的所有单元格
                for (int col = 0; col < rowData.length; col++) {
                    String cellValue = rowData[col];
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

    /**
     * 将指定行及后续行向下移动指定行数
     *
     * @param startRow 起始行号
     * @param shiftCount 要移动的行数
     */
    private void shiftRowsDown(int startRow, int shiftCount) {
        int lastRowNum = getDataLastRowNum();
        if (startRow > lastRowNum) {
            return;
        }
        sheet.shiftRows(startRow, lastRowNum, shiftCount);
    }

    /**
     * @return last row contained on this sheet (0-based) or -1 if no row exists
     */
    private int getDataLastRowNum() {
        return sheet.getLastRowNum();
    }

}