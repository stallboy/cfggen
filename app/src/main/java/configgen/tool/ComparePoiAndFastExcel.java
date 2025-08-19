package configgen.tool;

import configgen.ctx.DirectoryStructure;
import configgen.data.*;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.CfgSchemas;
import configgen.util.Logger;

import java.nio.file.Path;
import java.util.List;

public class ComparePoiAndFastExcel {

    public static void compareCellData(CfgData dataByPoi, CfgData dataByFastExcel) {
        int notMatchCount = 0;

        for (CfgData.DTable tableByFastExcel : dataByFastExcel.tables().values()) {
            String tableName = tableByFastExcel.tableName();
            CfgData.DTable tableByPoi = dataByPoi.tables().get(tableName);
            if (tableByPoi == null) {
                Logger.log("table not found: %s", tableName);
                continue;
            }

            for (int i = 0; i < tableByPoi.rows().size(); i++) {
                List<CfgData.DCell> rowByFastExcel = tableByFastExcel.rows().get(i);
                List<CfgData.DCell> rowByPoi = tableByPoi.rows().get(i);
                if (rowByPoi == null) {
                    Logger.log("table: %s row not found: %d", tableName, i);
                    continue;
                }

                for (int j = 0; j < rowByFastExcel.size(); j++) {
                    CfgData.DCell cellByFastExcel = rowByFastExcel.get(j);
                    CfgData.DCell cellByPoi = rowByPoi.get(j);
                    boolean notMatch = notMatch(cellByFastExcel, cellByPoi);
                    if (notMatch) {
                        Logger.log("%s     %s", cellByFastExcel, cellByPoi.value());
                        notMatchCount++;
                    }
                }
            }
        }

        Logger.log("not match count: %d", notMatchCount);
    }

    private static boolean notMatch(CfgData.DCell cellByFastExcel, CfgData.DCell cellByPoi) {
        String v1 = cellByFastExcel.value();
        String v2 = cellByPoi.value();

        boolean notMatch = false;
        boolean numberChecked = false;
        try {
            float f1 = Float.parseFloat(v1);
            float f2 = Float.parseFloat(v2);
            if (Math.abs(f1 - f2) > 0.01) {
                notMatch = true;
            }
            numberChecked = true;
        } catch (NumberFormatException ignored) {
        }

        if (!numberChecked && !v1.equalsIgnoreCase(v2)) {
            notMatch = true;
        }
        return notMatch;
    }

}

