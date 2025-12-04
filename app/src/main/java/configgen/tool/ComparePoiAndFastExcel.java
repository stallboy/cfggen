package configgen.tool;

import configgen.ctx.DirectoryStructure;
import configgen.ctx.HeadRow;
import configgen.ctx.HeadRows;
import configgen.data.*;
import configgen.gen.BuildSettings;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.schema.CfgSchema;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.CfgSchemas;
import configgen.util.Logger;

import java.nio.file.Path;
import java.util.List;

public class ComparePoiAndFastExcel extends Tool {
    private final Path dataDir;
    private final String csvDefaultEncoding;
    private final HeadRow headRow;


    public ComparePoiAndFastExcel(Parameter parameter) {
        super(parameter);
        dataDir = Path.of(parameter.get("datadir", "."));
        csvDefaultEncoding = parameter.get("csvencoding", "GBK");
        headRow = HeadRows.getById(parameter.get("headrow", "2"));

        parameter.title("compare fastexcel lib read to poi lib read");
    }

    @Override
    public void call() {
        DirectoryStructure sourceStructure = new DirectoryStructure(dataDir);
        CfgSchema schema = CfgSchemas.readFromDir(sourceStructure);
        Logger.profile("schema read");
        CfgSchemaErrs errs = schema.resolve();
        if (!errs.errs().isEmpty()) {
            errs.checkErrors();
        }
        ReadCsv csvReader = new ReadCsv(csvDefaultEncoding);
        CfgDataReader poiDataReader = new CfgDataReader(headRow, csvReader, BuildSettings.getPoiReader());
        CfgDataReader fastDataReader = new CfgDataReader(headRow, csvReader, ReadByFastExcel.INSTANCE);

        CfgSchemaErrs dataErrs = CfgSchemaErrs.of();
        CfgData dataByPoi = poiDataReader.readCfgData(sourceStructure, schema, dataErrs);
        dataErrs.checkErrors();

        for (int i = 0; i < 10; i++) {
            CfgSchemaErrs fastErr = CfgSchemaErrs.of();
            CfgData dataByFastExcel = fastDataReader.readCfgData(sourceStructure, schema, fastErr);
            fastErr.checkErrors();
            compareCellData(dataByPoi, dataByFastExcel);
        }
    }

    private static void compareCellData(CfgData dataByPoi, CfgData dataByFastExcel) {
        int notMatchCount = 0;

        for (CfgData.DTable tableByFastExcel : dataByFastExcel.tables().values()) {
            String tableName = tableByFastExcel.tableName();
            CfgData.DTable tableByPoi = dataByPoi.getDTable(tableName);
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

