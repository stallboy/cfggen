package configgen.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ExcelReader {

    record AllResult(List<OneSheetResult> sheets,
                     DataStat stat) {
    }

    record OneSheetResult(String tableName,
                          CfgData.DRawSheet sheet) {
    }


    AllResult readExcels(Path path, Path relativePath) throws IOException;
}
