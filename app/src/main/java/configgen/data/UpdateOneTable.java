package configgen.data;

import configgen.ctx.Context;
import configgen.ctx.HeadRow;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DTable;
import configgen.data.DataUtil.TableNameIndex;
import configgen.schema.CfgSchemaErrs;
import configgen.schema.SchemaUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static configgen.data.DataUtil.getTableNameIndex;

public class UpdateOneTable {

    public static CfgData update(@NotNull Context context,
                                 @NotNull DTable dTable) {


        DTable newTable = DTable.of(dTable.tableName(), new ArrayList<>(), dTable.nullableAddTag());
        Path rootDir = context.contextCfg().dataDir();
        for (DRawSheet sheet : dTable.rawSheets()) {
            Path path = rootDir.resolve(sheet.fileName());
            Path relativePath = Path.of(sheet.fileName());
            DataUtil.FileFmt fmt = DataUtil.getFileFormat(path);
            if (fmt == null) {
                throw new IllegalArgumentException("Unknown file: " + relativePath);
            }
            ReadResult result = null;
            switch (fmt) {
                case CSV, TXT_AS_TSV -> {
                    TableNameIndex ti = getTableNameIndex(relativePath);
                    if (ti == null) {
                        throw new IllegalArgumentException("Not legal path: " + relativePath);
                    }
                    char fieldSeparator = fmt == DataUtil.FileFmt.CSV ? ',' : '\t';
                    result = context.csvReader().readCsv(path, relativePath,
                            ti.tableName(), ti.index(), fieldSeparator, dTable.nullableAddTag());
                }

                case EXCEL -> {
                    result = context.excelReader().readExcels(path, relativePath, sheet.sheetName());
                }
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

        if (!errs.errs().isEmpty()) {
            throw new IllegalArgumentException("Errors when updating table: " + dTable.tableName()
                    + ", errs: " + errs);
        }

        CfgData cfgData = context.cfgData();
        var newTables = new HashMap<>(cfgData.tables());
        newTables.put(newTable.tableName(), newTable);
        return new CfgData(newTables, cfgData.stat());
    }
}
