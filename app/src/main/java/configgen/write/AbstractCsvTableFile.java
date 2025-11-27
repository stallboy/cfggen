package configgen.write;

import configgen.util.CSVUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * CSV表格文件抽象基类
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