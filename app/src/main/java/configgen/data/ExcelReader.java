package configgen.data;

import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public interface ExcelReader {

    /**
     * @param sheet 指定读取的sheet名称，null表示读取所有sheet
     */
    ReadResult readExcels(Path path,
                          Path relativePath,
                          @Nullable String sheet) ;
}
