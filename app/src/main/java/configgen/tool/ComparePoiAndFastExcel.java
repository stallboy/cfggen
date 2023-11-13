package configgen.tool;

import configgen.data.CfgData;
import configgen.data.CfgDataReader;
import configgen.schema.CfgSchema;
import configgen.schema.SchemaErrs;
import configgen.schema.cfg.Cfgs;
import configgen.util.Logger;

import java.nio.file.Path;
import java.util.List;

public class ComparePoiAndFastExcel {

    public static void compareCellData(Path dataDir, int headRow, String defaultEncoding) {
        Path cfgPath = dataDir.resolve("config.cfg");
        CfgSchema schema = Cfgs.readFrom(cfgPath, true);
        Logger.profile("schema read");
        SchemaErrs errs = schema.resolve();
        if (!errs.errs().isEmpty()) {
            errs.print("schema");
        }

        CfgData dataByFastExcel = CfgDataReader.INSTANCE.readCfgData(dataDir, schema, headRow, false, defaultEncoding);

        CfgData dataByPoi = CfgDataReader.INSTANCE.readCfgData(dataDir, schema, headRow, true, defaultEncoding);


        int notMatchCount = 0;

        for (CfgData.DTable tableByFastExcel : dataByFastExcel.tables().values()) {
            String tableName = tableByFastExcel.tableName();
            CfgData.DTable tableByPoi = dataByPoi.tables().get(tableName);
            if (tableByPoi == null) {
                Logger.log(STR. "table not found: \{ tableName }" );
                continue;
            }

            for (int i = 0; i < tableByPoi.rows().size(); i++) {
                List<CfgData.DCell> rowByFastExcel = tableByFastExcel.rows().get(i);
                List<CfgData.DCell> rowByPoi = tableByPoi.rows().get(i);
                if (rowByPoi == null) {
                    Logger.log(STR. "\{ tableName } row not found: \{ i }" );
                    continue;
                }

                for (int j = 0; j < rowByFastExcel.size(); j++) {
                    CfgData.DCell cellByFastExcel = rowByFastExcel.get(j);
                    CfgData.DCell cellByPoi = rowByPoi.get(j);

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
                    } catch (NumberFormatException _) {
                    }

                    if (!numberChecked && !v1.equalsIgnoreCase(v2)) {
                        notMatch = true;
                    }

                    if (notMatch) {
                        Logger.log(STR. "\{ cellByFastExcel }     \{ cellByPoi.value() }" );
                        notMatchCount++;
                    }

                }
            }
        }

        Logger.log(STR. "not match count: \{ notMatchCount }" );
    }

}

