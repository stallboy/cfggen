package configgen.write;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV表格文件实现（列模式）
 */
public class ColumnModeCsvTableFile extends AbstractCsvTableFile {
    public ColumnModeCsvTableFile(Path filePath, String defaultEncoding, int headRow) {
        super(filePath, defaultEncoding, headRow);
    }

    @Override
    protected int lineCount() {
        return getColumnCount();
    }

    @Override
    protected void emptyCell(int line, int index) {
        if (index >= 0 && index < rows.size()) {
            List<String> row = rows.get(index);
            if (line < row.size()) {
                row.set(line, "");
            }
        }
    }

    @Override
    protected void emptyLine(int line) {
        for (List<String> row : rows) {
            if (line < row.size()) {
                row.set(line, "");
            }
        }
    }

    @Override
    protected void insertGapLines(int from, int count) {
        for (int i = 0; i < count; i++) {
            insertColumn(from + i);
        }
    }

    @Override
    protected void ensureCapacity(int required) {
        // 确保有足够的列
        int currentMaxCols = getColumnCount();
        if (required <= currentMaxCols) {
            return;
        }

        // 为所有行添加缺失的列
        for (List<String> row : rows) {
            while (row.size() < required) {
                row.add("");
            }
        }
    }

    @Override
    protected void writeLine(int lineNum, String[] lineData, int capacity) {
        int newRowColCount = Math.max(getColumnCount(), capacity);
        // 确保有足够的行（block 的行数可能超过 CSV 现有行数）
        while (rows.size() < lineData.length) {
            rows.add(createEmptyRow(newRowColCount));
        }
        // 写入该列的所有单元格
        for (int row = 0; row < lineData.length; row++) {
            String cellValue = lineData[row];
            if (cellValue != null) {
                List<String> rowData = rows.get(row);
                rowData.set(lineNum, cellValue);
            }
        }
    }

    private List<String> createEmptyRow(int columnCount) {
        return new ArrayList<>(Collections.nCopies(columnCount, ""));
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

    private int getColumnCount() {
        return rows.isEmpty() ? 0 : rows.getFirst().size();
    }
}
