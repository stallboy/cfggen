package configgen.write;

import configgen.util.CSVUtil;
import configgen.write.RecordBlock.RecordBlockTransformed;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * CSV表格文件抽象基类
 * <p>
 * 行模式与列模式的 {@link #emptyRows} / {@link #insertRecordBlock} 共用同一骨架，
 * 方向相关的操作由子类通过钩子方法提供（行模式下"线"=行，列模式下"线"=列）。
 */
public abstract class AbstractCsvTableFile implements TableFile {
    protected final Path filePath;
    protected final List<List<String>> rows;
    protected final int headRow;
    protected boolean modified = false;

    public AbstractCsvTableFile(@NotNull Path filePath,
                                @NotNull String defaultEncoding,
                                int headRow)  {
        if (headRow < 0) {
            throw new IllegalArgumentException("headRow must be non-negative");
        }

        this.filePath = filePath;
        this.headRow = headRow;

        try {
            this.rows = CSVUtil.readAndNormalize(filePath, defaultEncoding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file: " + filePath, e);
        }
    }

    /**
     * 清空指定范围的数据（行模式下清空行，列模式下清空列）
     *
     * @param startLine 起始行/列号（从0开始）
     * @param count    要清空的行/列数
     * @param fieldIndices 如果为null表示第一行/列全部清空，如果不为null表示第一行/列只清空指定indices下的数据
     */
    @Override
    public void emptyRows(int startLine, int count, List<Integer> fieldIndices) {
        int lineCount = lineCount();
        if (startLine < 0 || count <= 0 || startLine >= lineCount) {
            return;
        }

        int end = Math.min(startLine + count, lineCount);
        // 清空指定范围内的行/列
        for (int i = startLine; i < end; i++) {
            if (i == startLine && fieldIndices != null) {
                // 只清空指定 indices下的数据
                for (int cellIndex : fieldIndices) {
                    emptyCell(i, cellIndex);
                }
            } else {
                // 清空行/列中的所有单元格
                emptyLine(i);
            }
        }
        markModified();
    }

    /**
     * 插入记录块到指定位置
     *
     * @param startLine 起始行/列号，-1表示放到最后
     * @param emptyLineCount 可用的空行/列数
     * @param content 记录块内容
     */
    @Override
    public void insertRecordBlock(int startLine, int emptyLineCount, @NotNull RecordBlockTransformed content) {
        int contentLineCount = content.getRowCount();
        if (contentLineCount <= 0) {
            return;
        }

        int actualStartLine;
        if (startLine == -1) {
            // 放到最后
            actualStartLine = Math.max(lineCount(), headRow);
        } else {
            actualStartLine = startLine;
        }

        // 如果内容行/列数大于可用空行/列数，需要插入新行/列
        if (contentLineCount > emptyLineCount && startLine != -1) {
            insertGapLines(actualStartLine + emptyLineCount, contentLineCount - emptyLineCount);
        }

        // 确保有足够的容量
        int required = actualStartLine + contentLineCount;
        ensureCapacity(required);

        // 写入记录块内容
        for (int lineOffset = 0; lineOffset < contentLineCount; lineOffset++) {
            String[] lineData = content.getRow(lineOffset);
            if (lineData != null) {
                writeLine(actualStartLine + lineOffset, lineData, required);
            }
        }
        markModified();
    }

    /**
     * @return 行模式下为行数，列模式下为列数
     */
    protected abstract int lineCount();

    /**
     * 清空指定行/列中指定位置的单元格（index越界时忽略）
     */
    protected abstract void emptyCell(int line, int index);

    /**
     * 清空指定行/列中的所有单元格
     */
    protected abstract void emptyLine(int line);

    /**
     * 在from位置插入count个空行/列
     */
    protected abstract void insertGapLines(int from, int count);

    /**
     * 确保容量达到required（行模式补齐行，列模式补齐列）
     */
    protected abstract void ensureCapacity(int required);

    /**
     * 把一行/列内容写入lineNum位置
     *
     * @param capacity 本次插入所需的总容量（= actualStartLine + contentLineCount），
     *                 列模式用它决定补齐新行的宽度
     */
    protected abstract void writeLine(int lineNum, String[] lineData, int capacity);

    /**
     * 保存文件并关闭所有资源
     */
    @Override
    public void saveAndClose() {
        if (!modified) {
            return;
        }

        try {
            CSVUtil.writeToFile(filePath.toFile(), rows);
            modified = false;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Csv file: " + filePath, e);
        }
    }

    /**
     * 标记文件为已修改状态
     */
    protected void markModified() {
        this.modified = true;
    }

}