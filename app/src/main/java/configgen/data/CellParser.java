package configgen.data;

import configgen.schema.CfgSchema;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.*;

final class CellParser {

    /**
     * 无head，去空行，去注释行，去注释列
     * 返回的是规整的相同列数的row
     */
    static void parse(CfgData.DTable table, DataStat stat, CfgSchema cfgSchema, int headRow) {
        parse(table, stat, HeadParser.isColumnMode(table, cfgSchema), headRow);
    }

    static void parse(CfgData.DTable table, DataStat stat, boolean isColumnMode, int headRow) {
        List<List<DCell>> result = null;
        if (!isColumnMode) {

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

                    List<DCell> logicRow = new ArrayList<>(sheet.fieldIndices().size());
                    DRowId logicRowId = new DRowId(sheet.fileName(), sheet.sheetName(), rowIndex);
                    for (int col : sheet.fieldIndices()) {
                        logicRow.add(new DCell(rawRow.cell(col), logicRowId, col));
                    }
                    if (isLogicRowNotAllEmpty(logicRow)) {
                        result.add(logicRow);
                    } else {
                        stat.ignoredRowCount++;
                    }
                }
                sheet.rows().clear();
            }

        } else { // column mode

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

                        List<DCell> logicRow = new ArrayList<>(sheet.fieldIndices().size());
                        DRowId logicRowId = new DRowId(sheet.fileName(), sheet.sheetName(), logicRowIdx);
                        for (int col : sheet.fieldIndices()) {
                            DRawRow rawRow = sheet.rows().get(col);
                            String val = rawRow.cell(logicRowIdx);
                            logicRow.add(new DCell(val, logicRowId, col));
                        }
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

    static boolean isLogicRowNotAllEmpty(List<DCell> row) {
        return row.stream().anyMatch((c) -> !c.value().isEmpty());
    }


}
