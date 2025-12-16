package configgen.data;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public interface ExcelReader {

    /**
     * @param sheet 指定读取的sheet名称，null表示读取所有sheet
     */
    ReadResult readExcels(@NotNull Path path,
                          @NotNull Path relativePath,
                          String sheet) ;
}
