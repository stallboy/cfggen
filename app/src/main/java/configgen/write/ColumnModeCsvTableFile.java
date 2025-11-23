package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * CSV表格文件实现（列模式）
 */
public class ColumnModeCsvTableFile extends AbstractCsvTableFile {
    public ColumnModeCsvTableFile(Path filePath, String defaultEncoding, int headRow)  {
        super(filePath, defaultEncoding, headRow);
    }

    /**
     * 清空指定列范围的数据
     * @param startCol 起始列号（从0开始）
     * @param count 要清空的列数
     */
    @Override
    public void emptyRows(int startCol, int count) {
        int maxColumnCount = getMaxColumnCount();
        if (startCol < 0 || count <= 0 || startCol >= maxColumnCount) {
            return;
        }

        int end = Math.min(startCol + count, maxColumnCount);
        // 清空指定范围内的列
        for (List<String> row : rows) {
            for (int col = startCol; col < end; col++) {
                if (col < row.size()) {
                    row.set(col, "");
                }
            }
        }
        markModified();
    }

    /**
     * 插入记录块到指定位置
     * @param startCol 起始列号，-1表示放到最后一列
     * @param emptyColCount 可用的空列数
     * @param content 记录块内容
     */
    @Override
    public void insertRecordBlock(int startCol, int emptyColCount, @NotNull RecordBlockTransformed content) {
        if (content.getRowCount() <= 0) {
            return;
        }

        int actualStartCol;
        if (startCol == -1) {
            // 放到最后一列
            actualStartCol = Math.max(getMaxColumnCount(), headRow);
        } else {
            actualStartCol = startCol;
        }

        int contentColCount = content.getRowCount();

        // 如果内容列数大于可用空列数，需要插入新列
        if (contentColCount > emptyColCount && startCol != -1) {
            int insertCount = contentColCount - emptyColCount;
            for (int i = 0; i < insertCount; i++) {
                insertColumn(actualStartCol + emptyColCount + i);
            }
        }

        // 确保有足够的列
        ensureColumns(actualStartCol + contentColCount);

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
                        List<String> rowData = rows.get(row);
                        rowData.set(colNum, cellValue);
                    }
                }
            }
        }
        markModified();
    }

    @Override
    public String getCell(int row, int col) {
        // 列模式下，行列坐标互换
        return getCellValue(col, row);
    }

    private void insertColumn(int colIndex) {
        for (List<String> row : rows) {
            if (colIndex < row.size()) {
                row.add(colIndex, "");
            } else {
                // 如果列索引超出当前行大小，直接添加到末尾
                row.add("");
            }
        }
    }

    /**
     * 确保有足够的列数
     * @param requiredCols 需要的列数
     */
    private void ensureColumns(int requiredCols) {
        int currentMaxCols = getMaxColumnCount();
        if (requiredCols <= currentMaxCols) {
            return;
        }

        // 为所有行添加缺失的列
        for (List<String> row : rows) {
            while (row.size() < requiredCols) {
                row.add("");
            }
        }
    }

    protected int getMaxColumnCount() {
        return rows.getFirst().size();
    }
}