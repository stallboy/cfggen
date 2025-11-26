package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV表格文件实现（行模式）
 */
public class CsvTableFile extends AbstractCsvTableFile {
    private final int fixedMaxColumnCount;

    public CsvTableFile(Path filePath, String defaultEncoding, int headRow) {
        super(filePath, defaultEncoding, headRow);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no data: " + filePath);
        }
        fixedMaxColumnCount = rows.getFirst().size();
    }

    /**
     * 清空指定行范围的数据
     * @param startRow 起始行号（从0开始）
     * @param count 要清空的行数
     */
    @Override
    public void emptyRows(int startRow, int count) {
        if (startRow < 0 || count <= 0 || startRow >= rows.size()) {
            return;
        }

        int end = Math.min(startRow + count, rows.size());

        // 清空指定范围内的行
        for (int i = startRow; i < end; i++) {
            List<String> row = rows.get(i);
            Collections.fill(row, "");
        }
        markModified();
    }

    /**
     * 插入记录块到指定位置
     * @param startRow 起始行号，-1表示放到最后
     * @param emptyRowCount 可用的空行数
     * @param content 记录块内容
     */
    @Override
    public void insertRecordBlock(int startRow, int emptyRowCount, @NotNull RecordBlockTransformed content) {
        if (content.getRowCount() <= 0) {
            return;
        }

        int actualStartRow;
        if (startRow == -1) {
            // 放到最后
            actualStartRow = Math.max(rows.size(), headRow);
        } else {
            actualStartRow = startRow;
        }

        int contentRowCount = content.getRowCount();

        // 如果内容行数大于可用空行数，需要插入新行
        if (contentRowCount > emptyRowCount && startRow != -1) {
            int insertCount = contentRowCount - emptyRowCount;
            for (int i = 0; i < insertCount; i++) {
                rows.add(actualStartRow + emptyRowCount + i, createEmptyRow());
            }
        }

        // 确保有足够的行
        while (rows.size() < actualStartRow + contentRowCount) {
            rows.add(createEmptyRow());
        }

        // 写入记录块内容
        for (int rowOffset = 0; rowOffset < contentRowCount; rowOffset++) {
            int rowNum = actualStartRow + rowOffset;
            List<String> row = rows.get(rowNum);

            // 写入该行的数据
            String[] rowData = content.getRow(rowOffset);
            if (rowData != null) {
                for (int col = 0; col < rowData.length; col++) {
                    String cellValue = rowData[col];
                    if (cellValue != null) {
                        row.set(col, cellValue);
                    }
                }
            }
        }
        markModified();
    }

    private List<String> createEmptyRow() {
        return new ArrayList<>(Collections.nCopies(fixedMaxColumnCount, ""));
    }

    @Override
    public String getCell(int row, int col) {
        return getCellValue(row, col);
    }

    @Override
    public int getMaxRowCount() {
        return rows.size();
    }
}