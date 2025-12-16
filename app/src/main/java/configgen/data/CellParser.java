package configgen.data;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.*;

public final class CellParser {

    /**
     * 去head，去空行，去注释行，去注释列
     * 清除sheet的 原始rows信息 --> 汇集到table的 rows中
     */
    public static void parse(CfgData.DTable table, CfgDataStat stat, int headRow, boolean isColumnMode) {
        List<List<DCell>> result = null;
        if (!isColumnMode) {

            for (DRawSheet sheet : table.rawSheets()) {
                if (result == null) {
                    int size = sheet.rows().size() - headRow;
                    if (size < 0) {
                        size = 0;
                    }
                    result = new ArrayList<>(size);
                }

                for (int rowIndex = headRow; rowIndex < sheet.rows().size(); rowIndex++) {
                    DRawRow rawRow = sheet.rows().get(rowIndex);
                    if (rawRow.cell(0).startsWith("#")) {
                        stat.ignoredRowCount++;
                        continue;
                    }

                    List<DCell> logicRow = getCellsInRowMode(sheet, rawRow, rowIndex);
                    if (isLogicRowNotAllEmpty(logicRow)) {
                        result.add(logicRow);
                    } else {
                        stat.ignoredRowCount++;
                    }
                }
                sheet.rows().clear();  // 清理内存
            }

        } else { // column mode

            for (DRawSheet sheet : table.rawSheets()) {
                int maxRow = sheet.rows().stream().mapToInt(DRawRow::count).max().orElse(0);
                if (maxRow > headRow) {
                    if (result == null) {
                        result = new ArrayList<>(maxRow - headRow);
                    }

                    DRawRow rawRowFirst = sheet.rows().getFirst();
                    for (int logicRowIdx = headRow; logicRowIdx < maxRow; logicRowIdx++) {
                        String d = rawRowFirst.cell(logicRowIdx);
                        if (d.startsWith("#")) {
                            stat.ignoredRowCount++;
                            continue;
                        }

                        List<DCell> logicRow = getCellsInColumnMode(sheet, logicRowIdx);
                        if (isLogicRowNotAllEmpty(logicRow)) {
                            result.add(logicRow);
                        } else {
                            stat.ignoredRowCount++;
                        }
                    }
                }
                sheet.rows().clear(); // 清理内存
            }
        }

        if (result == null || result.isEmpty()) {
            stat.emptyTableCount++;
        } else {
            stat.rowCount = result.size();
            table.rows().clear();
            table.rows().addAll(result);
        }
    }

    public static void parse(CfgData.DTable table, CfgDataStat stat, int headRow) {
        parse(table, stat, headRow, false);
    }

    private static List<DCell> getCellsInColumnMode(DRawSheet sheet, int logicRowIdx) {
        List<DCell> logicRow = new ArrayList<>(sheet.fieldIndices().size());
        DRowId logicRowId = new DRowId(sheet.fileName(), sheet.sheetName(), logicRowIdx);
        for (int col : sheet.fieldIndices()) {
            DRawRow rawRow = sheet.rows().get(col);
            String val = rawRow.cell(logicRowIdx);
            boolean isNumber = rawRow.isCellNumber(logicRowIdx);
            logicRow.add(new DCell(val, logicRowId, col, DCell.modeOf(true, isNumber)));
        }
        return logicRow;
    }

    private static List<DCell> getCellsInRowMode(DRawSheet sheet, DRawRow rawRow, int rowIndex) {
        List<DCell> logicRow = new ArrayList<>(sheet.fieldIndices().size());
        DRowId logicRowId = new DRowId(sheet.fileName(), sheet.sheetName(), rowIndex);
        for (int col : sheet.fieldIndices()) {
            String val = rawRow.cell(col);
            boolean isNumber = rawRow.isCellNumber(col);
            logicRow.add(new DCell(val, logicRowId, col, DCell.modeOf(false, isNumber)));
        }
        return logicRow;
    }

    private static boolean isLogicRowNotAllEmpty(List<DCell> row) {
        return row.stream().anyMatch((c) -> !c.value().isEmpty());
    }


}
