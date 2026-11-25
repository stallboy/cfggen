package configgen.write;

import java.nio.file.Path;

public final class PoiExcelTableFileProvider implements ExcelTableFileProvider {
    @Override
    public TableFile open(Path filePath, String sheetName, int headRow, boolean columnMode) {
        return columnMode
                ? new ColumnModeExcelTableFile(filePath, sheetName, headRow)
                : new ExcelTableFile(filePath, sheetName, headRow);
    }
}
