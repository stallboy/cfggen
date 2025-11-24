package configgen.data;

import configgen.util.Logger;
import org.dhatim.fastexcel.reader.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public enum ReadByFastExcel implements ExcelReader {
    INSTANCE;

    @Override
    public AllResult readExcels(Path path, Path relativePath) throws IOException {
        CfgDataStat stat = new CfgDataStat();
        List<OneSheetResult> sheets = new ArrayList<>();

        stat.excelCount++;
        try (ReadableWorkbook wb = new ReadableWorkbook(path.toFile(),
                new ReadingOptions(true, false))) {
            for (Sheet sheet : wb.getSheets().toList()) {
                String sheetName = sheet.getName().trim();
                DataUtil.TableNameIndex ti = DataUtil.getTableNameIndex(relativePath, sheetName);
                if (ti == null) {
                    Logger.verbose2("%s [%s] 名字不符合规范，ignore！", path, sheetName);
                    stat.ignoredSheetCount++;
                    continue;
                }

                stat.sheetCount++;
                List<Row> rawRows = sheet.read();
                int formula = addStatAndReturnFormulaCount(rawRows, stat);
                if (formula > 0) {
                    Logger.verbose2("%s [%s] formula count=%d", path, sheetName, formula);
                }

                // rawRows会忽略空行，这里要fix下
                List<CfgData.DRawRow> rows = fixRows(rawRows);

                OneSheetResult oneSheet = new OneSheetResult(ti.tableName(),
                        new CfgData.DRawSheet(relativePath.toString(), sheetName, ti.index(), rows, new ArrayList<>()));
                sheets.add(oneSheet);
            }
        }
        return new AllResult(sheets, stat, null);
    }


    private List<CfgData.DRawRow> fixRows(List<Row> rawRows) {
        int rowCount = rawRows.getLast().getRowNum(); // getRowNum is 1 based
        List<CfgData.DRawRow> result = new ArrayList<>(rowCount);
        for (Row r : rawRows) {
            int rawRowIdx = r.getRowNum() - 1;
            if (result.size() > rawRowIdx) {
                throw new IllegalStateException("raw rows out of order");
            }
            while (result.size() < rawRowIdx) {
                result.add(EMPTY_ROW);
            }
            result.add(new DRawExcelRow(r));
        }
        return result;
    }


    private static int addStatAndReturnFormulaCount(List<Row> rawRows, CfgDataStat stat) {
        int formula = 0;
        for (Row row : rawRows) {
            for (Cell cell : row) {
                if (cell != null) {
                    CellType type = cell.getType();
                    switch (type) {
                        case NUMBER -> {
                            stat.cellNumberCount++;
                        }
                        case STRING -> {
                            stat.cellStrCount++;
                        }
                        case FORMULA -> {
                            stat.cellFormulaCount++;
                        }
                        case ERROR -> {
                            stat.cellErrCount++;
                        }
                        case BOOLEAN -> {
                            stat.cellBoolCount++;
                        }
                        case EMPTY -> {
                            stat.cellEmptyCount++;
                        }
                    }

                    if (type == CellType.FORMULA) {
                        formula++;
                    }

                } else {
                    stat.cellNullCount++;
                }
            }
        }
        return formula;


    }

    record DRawExcelRow(Row row) implements CfgData.DRawRow {
        @Override
        public String cell(int c) {
            return row.getCellText(c).trim();
        }

        @Override
        public boolean isCellNumber(int c) {
            Optional<Cell> cell = row.getOptionalCell(c);
            return cell.isPresent() && cell.get().getType() == CellType.NUMBER; //这里只判断数字，外部判断comma
        }

        @Override
        public int count() {
            return row.getCellCount();
        }
    }

    static CfgData.DRawRow EMPTY_ROW = new CfgData.DRawRow() {
        @Override
        public String cell(int c) {
            return "";
        }

        @Override
        public boolean isCellNumber(int c) {
            return false;
        }

        @Override
        public int count() {
            return 0;
        }
    };
}
