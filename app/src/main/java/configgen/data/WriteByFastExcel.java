package configgen.data;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import configgen.util.Logger;
import configgen.schema.*;
import configgen.value.CfgValue;
import configgen.value.CfgValue.VTable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum WriteByFastExcel implements ExcelReader {
    INSTANCE;

    @Override
    public WriteResult writeRecord(String tableName, Map<String, Object> data, Path excelDir) throws IOException {
        return writeRecord(tableName, data, excelDir, null);
    }

    public WriteResult writeRecord(String tableName, Map<String, Object> data, Path excelDir, CfgValue cfgValue) throws IOException {
        Path excelPath = excelDir.resolve(tableName + ".xlsx");

        try (Workbook wb = new Workbook(new FileOutputStream(excelPath.toFile()), "cfggen", "1.0")) {
            Worksheet ws = wb.newWorksheet(tableName);

            // 如果有表结构信息，按照表格映射规则写入
            if (cfgValue != null) {
                VTable vTable = cfgValue.vTableMap().get(tableName);
                if (vTable != null) {
                    return writeWithSchema(ws, tableName, data, vTable, excelPath);
                }
            }

            // 如果没有表结构信息，使用简单写入
            return writeSimple(ws, tableName, data, excelPath);
        } catch (Exception e) {
            Logger.log("Error writing to Excel: " + e.getMessage());
            return new WriteResult(tableName, excelPath, false, e.getMessage());
        }
    }

    private WriteResult writeWithSchema(Worksheet ws, String tableName, Map<String, Object> data,
                                       VTable vTable, Path excelPath) {
        TableSchema tableSchema = (TableSchema) vTable.schema();

        // 根据表格映射规则计算列布局
        TableLayoutCalculator calculator = new TableLayoutCalculator(tableSchema);
        List<ColumnInfo> columns = calculator.calculateLayout();

        // 写入表头
        int col = 0;
        for (ColumnInfo column : columns) {
            ws.value(0, col, column.getHeader());
            col++;
        }

        // 写入数据
        DataMapper mapper = new DataMapper(tableSchema, columns);
        List<Object> rowData = mapper.mapDataToRow(data);

        col = 0;
        for (Object value : rowData) {
            ws.value(1, col, value != null ? value.toString() : "");
            col++;
        }

        Logger.log("Successfully wrote record to Excel with schema mapping: " + excelPath);
        return new WriteResult(tableName, excelPath, true, "Success");
    }

    private WriteResult writeSimple(Worksheet ws, String tableName, Map<String, Object> data, Path excelPath) {
        // 简单写入：字段名作为表头，值作为数据
        int col = 0;
        for (String fieldName : data.keySet()) {
            ws.value(0, col, fieldName);
            col++;
        }

        col = 0;
        for (Object value : data.values()) {
            ws.value(1, col, value != null ? value.toString() : "");
            col++;
        }

        Logger.log("Successfully wrote record to Excel: " + excelPath);
        return new WriteResult(tableName, excelPath, true, "Success");
    }

    @Override
    public List<WriteResult> writeRecords(String tableName, List<Map<String, Object>> dataList, Path excelDir) throws IOException {
        List<WriteResult> results = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            Map<String, Object> data = dataList.get(i);
            WriteResult result = writeRecord(tableName, data, excelDir);
            results.add(result);

            if (!result.success()) {
                Logger.log("Failed to write record " + i + " for table " + tableName + ": " + result.message());
            }
        }

        return results;
    }

    @Override
    public AllResult readExcels(Path path, Path relativePath) throws IOException {
        // 复用现有的读取实现
        return ReadByFastExcel.INSTANCE.readExcels(path, relativePath);
    }
}