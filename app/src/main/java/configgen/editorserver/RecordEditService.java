package configgen.editorserver;

import configgen.ctx.Context;
import configgen.value.*;
import configgen.write.AddOrUpdateService;
import configgen.write.AddOrUpdateService.AddOrUpdateRecordResult;
import configgen.write.DeleteService;
import configgen.write.DeleteService.DeleteRecordResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static configgen.editorserver.RecordEditService.ResultCode.*;

public final class RecordEditService {

    public enum ResultCode {
        addOk,
        updateOk,
        deleteOk,

        serverNotEditable,
        tableNotSet,
        idNotSet,
        tableNotFound,
        idParseErr,
        idNotFound,
        jsonParseErr,
        storeErr,
    }

    public record RecordEditResult(ResultCode resultCode,
                                   String table,
                                   String id,
                                   List<String> valueErrs) {// 即使有错，也更新，只是这里提示
    }

    public record ResultWithNewCfgValue(RecordEditResult result,
                                        CfgValue newCfgValue) {
    }


    public static ResultWithNewCfgValue addOrUpdateRecord(@NotNull Context context,
                                                          @NotNull CfgValue cfgValue,
                                                          String table,
                                                          @NotNull String jsonStr) {


        if (table == null) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(tableNotSet, "", "", List.of()),
                    null);
        }

        AddOrUpdateRecordResult ar = AddOrUpdateService.addOrUpdateRecord(context, cfgValue, table, jsonStr);
        ResultCode resultCode = switch (ar.errorCode()) {
            case AddOK -> addOk;
            case UpdateOK -> updateOk;
            case PartialNotEditable -> serverNotEditable;
            case TableNotFound -> tableNotFound;
            case RecordParseError -> jsonParseErr;
            case IOException -> storeErr;
        };

        return new ResultWithNewCfgValue(
                new RecordEditResult(resultCode, table, ar.recordId(), ar.errorMessages()),
                ar.newCfgValue());
    }


    public static ResultWithNewCfgValue deleteRecord(@NotNull Context context,
                                                     @NotNull CfgValue cfgValue,
                                                     String table,
                                                     String id) {
        if (table == null) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(tableNotSet, "", "", List.of()),
                    null);
        }

        if (id == null) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(idNotSet, table, "", List.of()),
                    null);
        }

        DeleteRecordResult dr = DeleteService.deleteRecord(context, cfgValue, table, id);
        ResultCode resultCode = switch (dr.errorCode()) {
            case OK -> deleteOk;
            case PartialNotEditable -> serverNotEditable;
            case TableNotFound -> tableNotFound;
            case RecordIdParseError -> idParseErr;
            case RecordIdNotFound -> idNotFound;
            case IOException -> storeErr;
        };
        return new ResultWithNewCfgValue(
                new RecordEditResult(resultCode, table, id, dr.errorMessages()),
                dr.newCfgValue());
    }

}
