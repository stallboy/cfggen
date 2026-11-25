package configgen.write;

import java.nio.file.Path;

public interface ExcelTableFileProvider {
    String MISSING_MESSAGE = "Excel support requires cfggen-excel-write.jar. "
            + "Place the matching JAR next to cfggen.jar and restart.";

    TableFile open(Path filePath, String sheetName, int headRow, boolean columnMode);
}
