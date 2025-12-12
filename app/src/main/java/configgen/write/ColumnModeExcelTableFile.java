package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public class ColumnModeExcelTableFile extends AbstractExcelTableFile {
    public ColumnModeExcelTableFile(@NotNull Path filePath,
                                    @NotNull String sheetName,
                                    int headRow) {
        super(filePath, sheetName, headRow);
    }


    /**
     * 清空指定行范围的数据
     *
     * @param startRow 起始行号（从0开始）
     * @param count    要清空的行数
     * @param fieldIndices 如果为null表示第一行全部清空，如果不为null表示第一行只清空指定indices下的数据
     */
    @Override
    public void emptyRows(int startRow, int count, List<Integer> fieldIndices) {
        if (startRow < 0 || count <= 0) {
            return;
        }

        // 列模式：清空指定列范围的数据
        int lastColNum = getColumnCount() - 1;
        if (startRow > lastColNum) {
            return;
        }

        int endColPlus1 = Math.min(startRow + count, lastColNum + 1);

        for (int col = startRow; col < endColPlus1; col++) {
            if (col == startRow && fieldIndices != null) {
                // 只清空指定 indices下的数据
                for (int rowIndex : fieldIndices) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        Cell cell = row.getCell(col);
                        if (cell != null) {
                            cell.setBlank();
                        }
                    }
                }
            } else {
                // 清空指定范围内的列
                for (Row row : sheet) {
                    Cell cell = row.getCell(col);
                    if (cell != null) {
                        cell.setBlank();
                    }
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

        // 列模式：插入记录块到指定列位置
        int actualStartCol;
        if (startRow == -1) {
            // 放到最后一列
            actualStartCol = Math.max(getColumnCount(), headRow);
        } else {
            actualStartCol = startRow;
        }

        int contentColCount = content.getRowCount();

        // 如果内容列数大于可用空列数，需要移动后续列
        if (contentColCount > emptyRowCount && startRow != -1) {
            shiftColumnsRight(startRow + emptyRowCount, contentColCount - emptyRowCount);
        }

        // 写入记录块内容（按列存储）
        for (int colOffset = 0; colOffset < contentColCount; colOffset++) {
            int colNum = actualStartCol + colOffset;

            // 获取RecordBlock中该列的数据
            String[] colData = content.getRow(colOffset);
            if (colData != null) {
                // 写入该列的所有单元格
                for (int row = 0; row < colData.length; row++) {
                    String cellValue = colData[row];
                    if (cellValue != null) {
                        Row sheetRow = sheet.getRow(row);
                        if (sheetRow == null) {
                            sheetRow = sheet.createRow(row);
                        }
                        Cell cell = sheetRow.getCell(colNum);
                        if (cell == null) {
                            cell = sheetRow.createCell(colNum);
                        }
                        cell.setCellValue(cellValue);
                    }
                }
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

    /**
     * 将指定列及后续列向右移动指定列数
     *
     * @param startCol 起始列号
     * @param shiftCount 要移动的列数
     */
    private void shiftColumnsRight(int startCol, int shiftCount) {
        int maxCols = getColumnCount();
        if (startCol >= maxCols) {
            return;
        }

        sheet.shiftColumns(startCol, maxCols - 1, shiftCount);
    }


}