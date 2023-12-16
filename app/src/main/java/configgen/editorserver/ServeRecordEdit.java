package configgen.editorserver;

import configgen.value.CfgValue;

import java.util.List;

public class ServeRecordEdit {

    public enum ResultCode {
        ok,
        tableNotFound,
        idNotFound,
        jsonFmtError,
    }

    public enum EditRequestType {
        add,
        update,
        delete
    }

    public record RecordEditResult(ResultCode resultCode,
                                   String err,
                                   EditRequestType request,
                                   String table,
                                   String id,
                                   List<ServeSchema.RecordId> recordIds) {
    }


    private final CfgValue cfgValue;

    public ServeRecordEdit(CfgValue cfgValue) {
        this.cfgValue = cfgValue;
    }

    public RecordEditResult addRecord(String jsonStr) {

        return null;

    }

    public RecordEditResult updateRecord(String jsonStr) {
        return null;

    }

    public RecordEditResult deleteRecord(String table, String id) {
        return null;

    }

}
