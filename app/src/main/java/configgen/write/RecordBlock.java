package configgen.write;

import java.util.Objects;

/**
 * RecordBlock接口用于表示记录块，提供单元格写入操作
 */
public class RecordBlock {
    private final int maxColumns;
    private String[][] cells;
    private int maxRow = -1;

    public RecordBlock(int maxColumns) {
        this.maxColumns = maxColumns;
        this.cells = new String[16][];
    }

    /**
     * 设置单元格的值
     * @param row 行号（从0开始）
     * @param col 列号（从0开始）
     * @param value 单元格值
     */
    public void setCell(int row, int col, String value) {
        Objects.requireNonNull(value);
        if (row < 0 || col < 0 || col >= maxColumns) {
            throw new IllegalArgumentException("Invalid row or column index");
        }
        expandIfNeeded(row);
        if (cells[row] == null) {
            cells[row] = new String[maxColumns];
        }
        cells[row][col] = value;
        if (row > maxRow) {
            maxRow = row;
        }
    }

    private void expandIfNeeded(int row) {
        int neededRows = row + 1;
        int len = cells.length;
        boolean needExpand = false;
        while (len < neededRows) {
            len *= 2;
            needExpand = true;
        }

        if (needExpand) {
            String[][] newCells = new String[len][];
            System.arraycopy(cells, 0, newCells, 0, cells.length);
            cells = newCells;
        }
    }

    public String[] getRow(int row) {
        if (row < 0 || row > maxRow) {
            throw new IllegalArgumentException("Invalid row index");
        }
        return cells[row];
    }

    /**
     * 获取记录块的行数
     * @return 行数
     */
    public int getRowCount() {
        return maxRow + 1;
    }
}