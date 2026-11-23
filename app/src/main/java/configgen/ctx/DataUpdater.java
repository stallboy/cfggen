package configgen.ctx;

import configgen.layout.FileFormat;
import configgen.data.CfgData;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DTable;
import configgen.data.CellParser;
import configgen.data.CfgDataStat;
import configgen.layout.TableFileName.TableNameIndex;
import configgen.data.HeadParser;
import configgen.data.HeadRow;
import configgen.data.ReadResult;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.SchemaUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static configgen.layout.TableFileName.getTableNameIndex;

public class DataUpdater {

    public record NewCfgDataResult(CfgData newCfgData,
                                   List<String> errStrList) {
    }

    public static NewCfgDataResult updateByReloadTable(Context context,
                                                       DTable dTable) {

        DTable newTable = DTable.of(dTable.tableName(), new ArrayList<>());
        Path rootDir = context.rootDir();
        for (DRawSheet sheet : dTable.rawSheets()) {
            Path path = rootDir.resolve(sheet.relativeFilePath());
            Path relativePath = Path.of(sheet.relativeFilePath());
            FileFormat fmt = FileFormat.getFileFormat(path);
            if (fmt == null) {
                throw new IllegalArgumentException("Unknown file: " + relativePath);
            }
            ReadResult result = null;
            switch (fmt) {
                case CSV -> {
                    TableNameIndex ti = getTableNameIndex(relativePath);
                    if (ti == null) {
                        throw new IllegalArgumentException("Not legal path: " + relativePath);
                    }
                    result = context.csvReader().readCsv(path, relativePath,
                            ti.tableName(), ti.index());
                }

                case EXCEL -> result = context.excelReader().readExcels(path, relativePath, sheet.sheetName());
            }
            Objects.requireNonNull(result);
            for (ReadResult.OneSheet oneSheet : result.sheets()) {
                newTable.rawSheets().add(oneSheet.sheet());
            }
        }


        CfgSchemaErrs errs = CfgSchemaErrs.of();
        CfgDataStat tStat = new CfgDataStat();
        boolean isColumnMode = SchemaUtil.isColumnMode(context.cfgSchema(), dTable.tableName());
        HeadRow headRow = context.contextCfg().headRow();
        HeadParser.parse(newTable, tStat, headRow, isColumnMode, errs);
        CellParser.parse(newTable, tStat, headRow.rowCount(), isColumnMode);

        CfgData cfgData = context.cfgData();
        var newTables = new HashMap<>(cfgData.tables());
        newTables.put(newTable.tableName(), newTable);
        return new NewCfgDataResult(new CfgData(newTables, cfgData.stat()),
                errs.errs().stream().map(Object::toString).toList());
    }
}
