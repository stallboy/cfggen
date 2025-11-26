package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
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
    void insertRecordBlock(int startRow, int emptyRowCount, @NotNull RecordBlockTransformed content);

    /**
     * 保存文件并关闭
     */
    void saveAndClose();


    /**
     * 获取指定单元格的值
     * @param row 行号（从0开始）
     * @param col 列号（从0开始）
     * @return null or 单元格的字符串值
     */
    String getCell(int row, int col);


    /**
     * @return 逻辑上的最大行数
     */
    int getMaxRowCount();

    /**
     * 确定一个record占的行数
     */
    default int findRecordRowCount(int startRow) {
        int max = getMaxRowCount();
        if (startRow >= max) {
            throw new IllegalArgumentException("startRow >= maxRowCount");
        }

        // 检查后续行，如果第一列为空，则继续计数
        int currentRow = startRow + 1;
        while (currentRow < max) {
            String firstCellValue = getCell(currentRow, 0);
            if (firstCellValue == null || firstCellValue.trim().isEmpty()) {
                currentRow++;
            } else {
                break;
            }
        }
        return currentRow - startRow;
    }


}