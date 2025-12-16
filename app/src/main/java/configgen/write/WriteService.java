package configgen.write;

import configgen.ctx.Context;
import configgen.ctx.DirectoryStructure.JsonFileInfo;
import configgen.data.CfgData.DTable;
import configgen.schema.TableSchema;
import configgen.value.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class WriteService {
    private final Context context;
    private final CfgValue cfgValue;

    public WriteService(Context context, CfgValue cfgValue) {
        this.context = context;
        this.cfgValue = cfgValue;
    }

    public enum AddOrUpdateErrorCode {
        OK,
        PartialNotEditable,
        TableNotFound,
        RecordParseError,
        IOException
    }

    public record AddOrUpdateRecordResult(AddOrUpdateErrorCode errorCode,
                                          String table,
                                          String recordId,
                                          List<String> errorMessages) {
    }

    public AddOrUpdateRecordResult addOrUpdateRecord(String tableName, String recordJsonStr) {

        if (cfgValue.schema().isPartial()) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.PartialNotEditable, tableName, null, List.of());
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.TableNotFound, tableName, null, List.of());
        }

        TableSchema tableSchema = vTable.schema();
        CfgValueErrs parseErrs = CfgValueErrs.of();
        CfgValue.VStruct thisValue = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(recordJsonStr);
        parseErrs.checkErrors("check json", true, true);
        if (!parseErrs.errs().isEmpty()) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.RecordParseError, tableName, null,
                    parseErrs.errs().stream().map(CfgValueErrs.VErr::toString).toList());
        }

        CfgValue.Value pkValue = ValueUtil.extractPrimaryKeyValue(thisValue, tableSchema);
        String id = pkValue.packStr();

        try {
            if (vTable.schema().isJson()) {
                Path jsonPath = VTableJsonStorage.addOrUpdateRecord(thisValue, tableName, id,
                        context.sourceStructure().getRootDir());

                JsonFileInfo jf = context.sourceStructure().addJsonFile(tableName, jsonPath);
                ValueUpdater.updateByJsonFileAddOrUpdate(context, cfgValue, vTable, jf);



            } else {
                DTable dTable = context.cfgData().getDTable(tableName);
                VTableStorage.addOrUpdateRecord(context, vTable, dTable, pkValue, thisValue);
                ValueUpdater.updateByReloadTableData(context, cfgValue, vTable);

            }


        } catch (IOException e) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.IOException, tableName, id,
                    List.of(e.getMessage()));
        }

        return null;
    }

}
