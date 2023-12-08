package configgen.data;

import configgen.util.Logger;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.apache.poi.ss.usermodel.CellType.NUMERIC;

public enum ReadByPoi implements ExcelReader {
    INSTANCE;

    record DRawPoiExcelRow(org.apache.poi.ss.usermodel.Row row,
                           DRawPoiFmt fmt) implements CfgData.DRawRow {
        @Override
        public String cell(int c) {
            Cell cell = row.getCell(c);
            if (cell != null) {
                return fmt.formatter.formatCellValue(cell, fmt.evaluator).trim();
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

    record DRawPoiFmt(DataFormatter formatter,
                      FormulaEvaluator evaluator) {
    }

    @Override
    public AllResult readExcels(Path path, Path relativePath) throws IOException {
        DataStat stat = new DataStat();
        List<OneSheetResult> sheets = new ArrayList<>();

        stat.excelCount++;


        try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DRawPoiFmt fmt = new DRawPoiFmt(formatter, evaluator);
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName().trim();
                DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(relativePath, sheetName);
                if (ti == null) {
                    Logger.verbose2("%s [%s] 名字不符合规范，ignore！", path, sheetName);
                    stat.ignoredSheetCount++;
                    continue;
                }

                stat.sheetCount++;
                List<CfgData.DRawRow> rows = new ArrayList<>(sheet.getLastRowNum() + 1);
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    rows.add(new DRawPoiExcelRow(row, fmt));
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
                OneSheetResult oneSheet = new OneSheetResult(ti.tableName(),
                        new CfgData.DRawSheet(relativePath.toString(), sheetName, ti.index(), rows, new ArrayList<>()));
                sheets.add(oneSheet);
            }
        }

        return new AllResult(sheets, stat);
    }


}
