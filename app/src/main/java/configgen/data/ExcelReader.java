package configgen.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ExcelReader {

    record AllResult(List<OneSheetResult> sheets,
                     CfgDataStat stat,
                     String nullableAddTag) {
    }

    record OneSheetResult(String tableName,
                          CfgData.DRawSheet sheet) {
    }

    AllResult readExcels(Path path, Path relativePath) throws IOException;
}
