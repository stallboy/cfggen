package configgen.write;

import java.util.List;
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
        this.cells = new String[4][];
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

    public static class RecordBlockTransformed {
        private final RecordBlock block;
        private final List<Integer> fieldIndices;
        private final int dataMaxColumns;

        public RecordBlockTransformed(RecordBlock block, List<Integer> fieldIndices) {
            this.block = block;
            this.fieldIndices = fieldIndices;
            this.dataMaxColumns = fieldIndices.getLast() + 1;
            if (block.maxColumns != fieldIndices.size()) {
                throw new IllegalArgumentException("fieldIndices size does not match block columns");
            }
        }


        public String[] getRow(int row) {
            if (row < 0 || row > block.maxRow) {
                throw new IllegalArgumentException("Invalid row index");
            }
            String[] rowCells = block.cells[row];
            if (rowCells == null) {
                return null;
            }
            String[] trans = new String[dataMaxColumns];
            for (int i = 0; i < block.maxColumns; i++) {
                String cell = rowCells[i];
                if (cell != null) {
                    int fi = fieldIndices.get(i);
                    trans[fi] = cell;
                }
            }
            return trans;
        }

        /**
         * 获取记录块的行数
         * @return 行数
         */
        public int getRowCount() {
            return block.maxRow + 1;
        }


    }

}