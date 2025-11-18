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

    // 写入结果记录
    record WriteResult(String tableName, Path excelPath, boolean success, String message) {
    }

    AllResult readExcels(Path path, Path relativePath) throws IOException;

    // 新增写入方法
    WriteResult writeRecord(String tableName, Map<String, Object> data, Path excelDir) throws IOException;

    // 批量写入方法
    List<WriteResult> writeRecords(String tableName, List<Map<String, Object>> dataList, Path excelDir) throws IOException;
}
