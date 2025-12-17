package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DTable;
import configgen.value.CfgValue;
import configgen.value.CfgValueErrs;
import configgen.value.ValuePack;
import configgen.write.ValueUpdater.NewCfgValueResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

public class DeleteService {

    public enum DeleteErrorCode {
        OK,
        PartialNotEditable,
        TableNotFound,
        RecordIdParseError,
        RecordIdNotFound,
        IOException
    }

    public record DeleteRecordResult(DeleteErrorCode errorCode,
                                     CfgValue newCfgValue,
                                     List<String> errorMessages) {
    }

    public static DeleteRecordResult deleteRecord(@NotNull Context context,
                                                  @NotNull CfgValue cfgValue,
                                                  @NotNull String tableName,
                                                  @NotNull String recordId) {

        if (cfgValue.schema().isPartial()) {
            return new DeleteRecordResult(DeleteErrorCode.PartialNotEditable, null, List.of());
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return new DeleteRecordResult(DeleteErrorCode.TableNotFound, null, List.of());
        }

        CfgValueErrs errs = CfgValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(recordId, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            return new DeleteRecordResult(DeleteErrorCode.RecordIdParseError, null,
                    errs.errs().stream().map(CfgValueErrs.VErr::toString).toList());
        }

        CfgValue.VStruct old = vTable.primaryKeyMap().get(pkValue);
        if (old == null) {
            return new DeleteRecordResult(DeleteErrorCode.RecordIdNotFound, null, List.of());
        }
        try {
            if (vTable.schema().isJson()) {
                Path relativeJsonPath = VTableJsonStorage.deleteRecord(tableName, recordId, context.rootDir());

                NewCfgValueResult nr = ValueUpdater.updateByJsonFileDelete(context, cfgValue, vTable, pkValue, recordId);

                context.sourceStructure().removeJsonFile(tableName, relativeJsonPath);
                context.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());

                return new DeleteRecordResult(DeleteErrorCode.OK,
                        nr.newCfgValue(), nr.errStrList());

            } else {
                DTable dTable = context.cfgData().getDTable(tableName);
                DRawSheet dRawSheet = VTableStorage.deleteRecord(context, dTable, old);

                NewCfgValueResult nr = ValueUpdater.updateByReloadTableData(context, cfgValue, vTable);

                context.sourceStructure().updateExcelFileLastModified(Path.of(dRawSheet.relativeFilePath()));
                context.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());

                return new DeleteRecordResult(DeleteErrorCode.OK, nr.newCfgValue(),
                        nr.errStrList());
            }
        } catch (Exception e) {
            return new DeleteRecordResult(DeleteErrorCode.IOException, null,
                    List.of(e.getMessage()));
        }
    }
}
