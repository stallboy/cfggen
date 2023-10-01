package configgen.data;

import configgen.schema.CfgSchema;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static configgen.data.CfgData.*;

final class CellParser {

    static void parse(CfgData.DTable table, DataStat stat, CfgSchema cfgSchema, int headRow) {
        parse(table, stat, HeadParser.isColumnMode(table, cfgSchema), headRow);
    }

    static void parse(CfgData.DTable table, DataStat stat, boolean isColumnMode, int headRow) {
        List<DCell[]> result = null;
        if (isColumnMode) {
            for (DRawSheet sheet : table.rawSheets()) {
                int maxRow = sheet.rows().stream().mapToInt(DRawRow::count).max().orElse(0);
                if (maxRow > headRow) {
                    if (result == null) {
                        result = new ArrayList<>(maxRow - headRow);
                    }

                    DRawRow rawRowFirst = sheet.rows().get(0);
                    for (int logicRowIdx = headRow; logicRowIdx < maxRow; logicRowIdx++) {
                        String d = rawRowFirst.cell(logicRowIdx);
                        if (d.startsWith("#")) {
                            stat.ignoredRowCount++;
                            continue;
                        }

                        DCell[] logicRow = new DCell[sheet.fieldIndices().size()];
                        DRowId logicRowId = new DRowId(sheet.fileName(), sheet.sheetName(), logicRowIdx);
                        int i = 0;
                        for (int col : sheet.fieldIndices()) {
                            DRawRow rawRow = sheet.rows().get(col);
                            String val = rawRow.cell(logicRowIdx);
                            logicRow[i] = new DCell(val, logicRowId, col);
                            i++;
                        }
                        if (isLogicRowNotAllEmpty(logicRow)) {
                            result.add(logicRow);
                        }
                    }
                }
                sheet.rows().clear(); // 清理内存
            }
        } else { // not column mode
            for (DRawSheet sheet : table.rawSheets()) {
                if (result == null) {
                    result = new ArrayList<>(sheet.rows().size() - headRow);
                }

                for (int rowIndex = headRow; rowIndex < sheet.rows().size(); rowIndex++) {
                    DRawRow rawRow = sheet.rows().get(rowIndex);
                    if (rawRow.cell(0).startsWith("#")) {
                        stat.ignoredRowCount++;
                        continue;
                    }

                    DCell[] logicRow = new DCell[sheet.fieldIndices().size()];
                    DRowId logicRowId = new DRowId(sheet.fileName(), sheet.sheetName(), rowIndex);
                    int i = 0;
                    for (int col : sheet.fieldIndices()) {
                        logicRow[i] = new DCell(rawRow.cell(col), logicRowId, col);
                        i++;
                    }
                    if (isLogicRowNotAllEmpty(logicRow)) {
                        result.add(logicRow);
                    }
                }
                sheet.rows().clear();
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

    static boolean isLogicRowNotAllEmpty(DCell[] row) {
        return Arrays.stream(row).anyMatch((c) -> !c.value().isEmpty());
    }


}
