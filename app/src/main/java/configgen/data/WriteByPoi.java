package configgen.data;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import configgen.util.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum WriteByPoi implements ExcelReader {
    INSTANCE;

    @Override
    public WriteResult writeRecord(String tableName, Map<String, Object> data, Path excelDir) throws IOException {
        Path excelPath = excelDir.resolve(tableName + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(tableName);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            int col = 0;
            for (String fieldName : data.keySet()) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(fieldName);
                col++;
            }

            // 创建数据行
            Row dataRow = sheet.createRow(1);
            col = 0;
            for (Object value : data.values()) {
                Cell cell = dataRow.createCell(col);
                if (value != null) {
                    cell.setCellValue(value.toString());
                }
                col++;
            }

            try (FileOutputStream outputStream = new FileOutputStream(excelPath.toFile())) {
                workbook.write(outputStream);
            }

            Logger.log("Successfully wrote record to Excel using POI: " + excelPath);
            return new WriteResult(tableName, excelPath, true, "Success");
        } catch (Exception e) {
            Logger.log("Error writing to Excel using POI: " + e.getMessage());
            return new WriteResult(tableName, excelPath, false, e.getMessage());
        }
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
        return ReadByPoi.INSTANCE.readExcels(path, relativePath);
    }
}