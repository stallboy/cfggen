package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DTable;
import configgen.schema.TableSchema;
import configgen.value.*;
import configgen.write.ValueUpdater.NewCfgValueResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AddOrUpdateService {

    public enum AddOrUpdateErrorCode {
        AddOK,
        UpdateOK,
        PartialNotEditable,
        TableNotFound,
        RecordParseError,
        IOException
    }

    public record AddOrUpdateRecordResult(AddOrUpdateErrorCode errorCode,
                                          String recordId,
                                          CfgValue newCfgValue,
                                          List<String> errorMessages) {
    }

    public static AddOrUpdateRecordResult addOrUpdateRecord(@NotNull Context context,
                                                            @NotNull CfgValue cfgValue,
                                                            @NotNull String tableName,
                                                            @NotNull String recordJsonStr) {

        if (cfgValue.schema().isPartial()) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.PartialNotEditable, null, null, List.of());
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.TableNotFound, null, null, List.of());
        }

        TableSchema tableSchema = vTable.schema();
        CfgValueErrs parseErrs = CfgValueErrs.of();
        CfgValue.VStruct thisValue = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(recordJsonStr);
        parseErrs.checkErrors("check json", true, true);
        if (!parseErrs.errs().isEmpty()) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.RecordParseError, null, null,
                    parseErrs.errs().stream().map(CfgValueErrs.VErr::toString).toList());
        }

        CfgValue.Value pkValue = ValueUtil.extractPrimaryKeyValue(thisValue, tableSchema);
        String id = pkValue.packStr();

        AddOrUpdateErrorCode code = vTable.primaryKeyMap().containsKey(pkValue) ?
                AddOrUpdateErrorCode.UpdateOK : AddOrUpdateErrorCode.AddOK;


        try {
            if (vTable.schema().isJson()) {
                Path relatveiJsonPath = VTableJsonStorage.addOrUpdateRecord(thisValue, tableName, id,
                        context.rootDir());

                NewCfgValueResult nr = ValueUpdater.updateByJsonFileAddOrUpdate(context, cfgValue, vTable, relatveiJsonPath);

                context.sourceStructure().addJsonFile(tableName, relatveiJsonPath);
                context.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());

                return new AddOrUpdateRecordResult(code, id,
                        nr.newCfgValue(), nr.errStrList());

            } else {
                DTable dTable = context.cfgData().getDTable(tableName);
                DRawSheet dRawSheet = VTableStorage.addOrUpdateRecord(context, vTable, dTable, pkValue, thisValue);

                NewCfgValueResult nr = ValueUpdater.updateByReloadTableData(context, cfgValue, vTable);

                context.sourceStructure().updateExcelFileLastModified(Path.of(dRawSheet.relativeFilePath()));
                context.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());

                return new AddOrUpdateRecordResult(code, id,
                        nr.newCfgValue(), nr.errStrList());
            }

        } catch (IOException e) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.IOException, id, null,
                    List.of(e.getMessage()));
        }
    }


}
