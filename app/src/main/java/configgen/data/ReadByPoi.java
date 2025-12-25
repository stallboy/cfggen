package configgen.data;

import configgen.data.ReadResult.OneSheet;
import configgen.util.Logger;
import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.apache.poi.ss.usermodel.CellType.FORMULA;
import static org.apache.poi.ss.usermodel.CellType.NUMERIC;

public enum ReadByPoi implements ExcelReader {
    INSTANCE;


    @Override
    public ReadResult readExcels(@NotNull Path path,
                                 @NotNull Path relativePath,
                                 String readSheet) {
        CfgDataStat stat = new CfgDataStat();
        List<OneSheet> sheets = new ArrayList<>();

        stat.excelCount++;


        try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName().trim();
                DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(relativePath, sheetName);
                if (ti == null) {
                    Logger.verbose2("%s [%s] 名字不符合规范，ignore！", path, sheetName);
                    stat.ignoredSheetCount++;
                    continue;
                }

                if (readSheet != null && !readSheet.equals(sheetName)) {
                    // 只用于更新，此时不管stat
                    continue;
                }


                stat.sheetCount++;
                List<CfgData.DRawRow> rows = new ArrayList<>(sheet.getLastRowNum() + 1);
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    rows.add(new DRawPoiExcelRow(row, formatter, evaluator));
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        switch (cell.getCellType()) {
                            case NUMERIC -> {
                                stat.cellNumberCount++;
                            }
                            case STRING -> {
                                stat.cellStrCount++;
                            }
                            case FORMULA -> {
                                stat.cellFormulaCount++;
                            }
                            case BLANK -> {
                                stat.cellEmptyCount++;
                            }
                            case BOOLEAN -> {
                                stat.cellBoolCount++;
                            }
                            case ERROR -> {
                                stat.cellErrCount++;
                            }
                        }
                    }
                }
                OneSheet oneSheet = new OneSheet(ti.tableName(),
                        new CfgData.DRawSheet(relativePath.toString(), sheetName, ti.index(), rows, new ArrayList<>()));
                sheets.add(oneSheet);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ReadResult(sheets, stat, null);
    }


    record DRawPoiExcelRow(org.apache.poi.ss.usermodel.Row row,
                           DataFormatter formatter,
                           FormulaEvaluator evaluator) implements CfgData.DRawRow {
        @Override
        public String cell(int c) {

            Cell cell = row.getCell(c);
            if (cell != null) {
                try {
                    return formatter.formatCellValue(cell, evaluator).trim();
                } catch (Throwable e) {
                    String res;
                    try {
                        res = cell.getStringCellValue();
                    } catch (Throwable e1) {
                        try {
                            res = cell.getNumericCellValue() + "";
                        } catch (Throwable e2) {
                            try {
                                res = cell.getBooleanCellValue() + "";
                            } catch (Throwable e3) {
                                res = "";
                            }
                        }
                    }

                    Logger.log("format cell failed: [%s] row %d col %d type %s formula %s use %s",
                            row.getSheet().getSheetName(), row.getRowNum(), c, cell.getCellType(),
                            cell.getCellType() == FORMULA ? cell.getCellFormula() : "",
                            res);

                    return res;
                }
            } else {
                return "";
            }
        }

        @Override
        public boolean isCellNumber(int c) {
            Cell cell = row.getCell(c);
            return cell != null && cell.getCellType() == NUMERIC; //这里只判断数字，外部判断comma
        }

        @Override
        public int count() {
            return row.getLastCellNum();
        }
    }
}
