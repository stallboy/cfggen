package configgen.data;

import configgen.layout.TableFileName;
import org.jspecify.annotations.Nullable;

import configgen.data.ReadResult.OneSheet;
import configgen.util.Logger;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.poi.ss.usermodel.CellType.FORMULA;

public enum ReadByPoi implements ExcelReader {
    INSTANCE;

    @Override
    public ReadResult readExcels(Path path,
                                 Path relativePath,
                                 @Nullable String readSheet) {
        CfgDataStat stat = new CfgDataStat();
        List<OneSheet> sheets = new ArrayList<>();

        stat.excelCount++;


        try (Workbook workbook = WorkbookFactory.create(path.toFile(), null, true)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                String sheetName = sheet.getSheetName().trim();
                TableFileName.TableNameIndex ti = TableFileName.getTableNameIndex(relativePath, sheetName);
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
                    while (rows.size() < row.getRowNum()) {
                        rows.add(CfgData.EMPTY_RAW_ROW);
                    }

                    // Evaluate and copy values before closing the workbook.
                    int width = Math.max(0, row.getLastCellNum());
                    int cellCount = row.getPhysicalNumberOfCells();
                    // POI iterates cells in column order. Keep a compact index for wide, sparse rows.
                    int @Nullable [] columns = width > 4 * cellCount ? new int[cellCount] : null;
                    @Nullable String[] cells = new String[columns == null ? width : cellCount];
                    int next = 0;
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        int index = cell.getColumnIndex();
                        if (columns != null) {
                            columns[next] = index;
                            index = next++;
                        }
                        cells[index] = formatCell(cell, formatter, evaluator);
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
                    rows.add(new DRawPoiExcelRow(width, columns, cells));
                }
                OneSheet oneSheet = new OneSheet(ti.tableName(),
                        new CfgData.DRawSheet(relativePath.toString(), sheetName, ti.index(), rows, new ArrayList<>()));
                sheets.add(oneSheet);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new ReadResult(sheets, stat);
    }


    private static String formatCell(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
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
                    cell.getSheet().getSheetName(), cell.getRowIndex(), cell.getColumnIndex(), cell.getCellType(),
                    cell.getCellType() == FORMULA ? cell.getCellFormula() : "",
                    res);

            return res;
        }
    }

    private record DRawPoiExcelRow(int count, int @Nullable [] columns, @Nullable String[] cells)
            implements CfgData.DRawRow {
        @Override
        public String cell(int c) {
            int index = columns == null ? c : Arrays.binarySearch(columns, c);
            if (index < 0 || index >= cells.length) {
                return "";
            }
            String value = cells[index];
            return value == null ? "" : value;
        }
    }
}
