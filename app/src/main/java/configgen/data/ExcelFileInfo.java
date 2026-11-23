package configgen.data;

import java.nio.file.Path;
import java.util.Objects;

public record ExcelFileInfo(long lastModified,
                            Path path,
                            Path relativePath,
                            DataUtil.FileFmt fmt) {
    public ExcelFileInfo {
        Objects.requireNonNull(path);
        Objects.requireNonNull(relativePath);
        Objects.requireNonNull(fmt);
    }
}
