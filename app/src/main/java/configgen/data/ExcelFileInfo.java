package configgen.data;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

public record ExcelFileInfo(long lastModified,
                            Path path,
                            Path relativePath,
                            DataUtil.FileFmt fmt,
                            @Nullable String nullableAddTag) {
    public ExcelFileInfo {
        Objects.requireNonNull(path);
        Objects.requireNonNull(relativePath);
        Objects.requireNonNull(fmt);
    }
}
