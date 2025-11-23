package configgen.write;

import org.jetbrains.annotations.NotNull;

/**
 * TableFile接口用于表示表格文件（Excel或CSV），提供写入操作
 */
public interface TableFile {

    /**
     * 清空指定行范围的数据
     * @param startRow 起始行号（从0开始）
     * @param count 要清空的行数
     */
    void emptyRows(int startRow, int count);

    /**
     * 插入记录块
     * @param startRow 起始行号，-1表示放到最后
     * @param emptyRowCount 可用的空行数
     * @param content 记录块内容
     */
    void insertRecordBlock(int startRow, int emptyRowCount, @NotNull RecordBlock content);

    /**
     * 保存文件并关闭
     */
    void saveAndClose();


    /**
     * 获取指定单元格的值
     * @param row 行号（从0开始）
     * @param col 列号（从0开始）
     * @return 单元格的字符串值
     */
    String getCell(int row, int col);

    /**
     * 确定一个record占的行数
     */
    default int findRecordRowCount(int startRow) {
        // 算法：从row开始，往下看每行第一cell如果为空，则行数+1
        int rowCount = 1;

        // 检查后续行，如果第一列为空，则继续计数
        int currentRow = startRow + 1;
        while (true) {
            String firstCellValue = getCell(currentRow, 0);
            if (firstCellValue == null || firstCellValue.trim().isEmpty()) {
                rowCount++;
                currentRow++;
            } else {
                break;
            }
        }
        return rowCount;
    }


}