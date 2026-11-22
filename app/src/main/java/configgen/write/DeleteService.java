package configgen.write;

import org.jspecify.annotations.Nullable;

import configgen.ctx.Context;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DTable;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.CfgValueErrs;
import configgen.value.ValuePack;
import configgen.write.ValueUpdater.NewCfgValueResult;

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
                                     @Nullable CfgValue newCfgValue,
                                     List<String> errorMessages) {
        public boolean committed() {
            return newCfgValue != null;
        }
    }

    public static DeleteRecordResult deleteRecord(Context context,
                                                  CfgValue cfgValue,
                                                  String tableName,
                                                  String recordId) {

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
            NewCfgValueResult nr;
            if (vTable.schema().isJson()) {
                var directoryStructure = context.sourceStructure();
                Path relativeJsonPath = VTableJsonStorage.deleteRecord(old, context.rootDir());
                nr = ValueUpdater.updateByJsonFileDelete(context, cfgValue, vTable, java.util.Objects.requireNonNull(pkValue), recordId);
                context.sourceStructure().removeJsonFile(tableName, relativeJsonPath);

            } else {
                DTable dTable = context.cfgData().getDTable(tableName);
                DRawSheet dRawSheet = VTableStorage.deleteRecord(context, java.util.Objects.requireNonNull(dTable), old);
                nr = ValueUpdater.updateByReloadTableData(context, cfgValue, vTable);
                context.sourceStructure().updateExcelFileLastModified(Path.of(dRawSheet.relativeFilePath()));
            }

            context.updateDataAndValue(nr.newCfgData(), nr.newCfgValue());

            Logger.log("Deleted record: table=%s, id=%s".formatted(tableName, recordId));
            return new DeleteRecordResult(DeleteErrorCode.OK,
                    nr.newCfgValue(), nr.errStrList());

        } catch (Exception e) {
            Logger.log("Failed to delete record: table=%s, id=%s, error=%s",
                    tableName, recordId, e.getMessage());
            return new DeleteRecordResult(DeleteErrorCode.IOException, null,
                    List.of(e.getMessage()));
        }
    }
}
