package configgen.write;

import configgen.write.RecordBlock.RecordBlockTransformed;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * TableFile接口用于表示表格文件（Excel或CSV），提供写入操作
 */
public interface TableFile {

    /**
     * 清空指定行范围的数据
     *
     * @param startRow 起始行号（从0开始）
     * @param count    要清空的行数
     * @param fieldIndices 如果为null表示第一行全部清空，如果不为null表示第一行只清空指定indices下的数据
     */
    void emptyRows(int startRow, int count, List<Integer> fieldIndices);

    /**
     * 插入记录块
     * @param startRow 起始行号，-1表示放到最后
     * @param emptyRowCount 可用的空行数，这些空行可以被覆盖
     * @param content 记录块内容，但空行不足以放置content时，需要插入新行
     */
    void insertRecordBlock(int startRow, int emptyRowCount, @NotNull RecordBlockTransformed content);

    /**
     * 保存文件并关闭
     */
    void saveAndClose();

}