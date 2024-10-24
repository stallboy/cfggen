package configgen.editorserver;

import configgen.data.Source;
import configgen.schema.Msg;
import configgen.schema.TableSchema;
import configgen.util.Logger;
import configgen.value.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static configgen.editorserver.RecordEditService.ResultCode.*;
import static configgen.value.CfgValue.*;

public class RecordEditService {

    public enum ResultCode {
        addOk,
        updateOk,
        deleteOk,

        serverNotEditable,
        tableNotSet,
        idNotSet,
        tableNotFound,
        tableNotEditable,
        idParseErr,
        idNotFound,
        jsonParseErr,
        jsonStoreErr,
    }

    public record RecordEditResult(
            ResultCode resultCode,
            String table,
            String id,
            List<String> valueErrs, // 即使有错，也更新，只是这里提示
            List<SchemaService.RecordId> recordIds) {
    }


    private final Path dataDir;
    private final CfgValue cfgValue;
    private CfgValue newCfgValue;

    public RecordEditService(Path dataDir, CfgValue cfgValue) {
        this.dataDir = dataDir;
        this.cfgValue = cfgValue;
    }

    public RecordEditResult addOrUpdateRecord(String table, String jsonStr) {
        RecordEditResult err = checkErr(table);
        if (err != null) {
            return err;
        }

        Logger.log("addOrUpdateRecord %s", jsonStr);
        VTable vTable = cfgValue.vTableMap().get(table);
        TableSchema tableSchema = vTable.schema();
        ValueErrs parseErrs = ValueErrs.of();
        VStruct thisValue = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(jsonStr, Source.DFile.of("<server>", table));
        parseErrs.checkErrors("check json", true, true);
        if (!parseErrs.errs().isEmpty()) {
            return new RecordEditResult(jsonParseErr, table, "",
                    parseErrs.errs().stream().map(Msg::msg).toList(), List.of());
        }


        Value pkValue = ValueUtil.extractPrimaryKeyValue(thisValue, tableSchema);
        String id = pkValue.packStr();

        List<VStruct> newRecordList;
        ResultCode code;
        VStruct old = vTable.primaryKeyMap().get(pkValue);
        if (old != null) {
            code = updateOk;
            Map<Value, VStruct> copy = new LinkedHashMap<>(vTable.primaryKeyMap());
            copy.put(pkValue, thisValue);
            newRecordList = copy.values().stream().toList();
        } else {
            code = addOk;
            newRecordList = new ArrayList<>(vTable.valueList().size() + 1);
            newRecordList.addAll(vTable.valueList());
            newRecordList.add(thisValue);
        }


        try {
            // 最后确定其他都对的时候再存储
            VTableJsonStore.addOrUpdateRecordStore(thisValue, tableSchema, id, dataDir, cfgValue.valueStat());
        } catch (Exception e) {
            return new RecordEditResult(jsonStoreErr, table, id, List.of(e.getMessage()), List.of());
        }
        ValueErrs errs = applyNewRecordList(newRecordList, tableSchema);
        List<String> errStrList = errs.errs().stream().map(Object::toString).toList();

        List<SchemaService.RecordId> recordIds = SchemaService.getRecordIds(newCfgValue.vTableMap().get(table));
        return new RecordEditResult(code, table, id, errStrList, recordIds);
    }

    private RecordEditResult checkErr(String table) {
        if (cfgValue.schema().isPartial()) {
            return ofErr(serverNotEditable, table);
        }

        if (table == null) {
            return ofErr(tableNotSet, null);
        }
        VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return ofErr(tableNotFound, table);
        }
        if (!vTable.schema().meta().isJson()) {
            return ofErr(tableNotEditable, table);
        }
        return null;
    }

    private RecordEditResult ofErr(ResultCode code, String table) {
        return new RecordEditResult(code, table == null ? "" : table, "", List.of(), List.of());
    }

    public CfgValue newCfgValue() {
        return newCfgValue;
    }

    private ValueErrs applyNewRecordList(List<VStruct> newRecordList, TableSchema tableSchema) {
        ValueErrs errs = ValueErrs.of();
        VTableCreator creator = new VTableCreator(tableSchema, errs);
        VTable newVTable = creator.create(newRecordList);

        Map<String, VTable> copy = new LinkedHashMap<>(cfgValue.vTableMap());
        copy.put(newVTable.name(), newVTable);
        newCfgValue = new CfgValue(cfgValue.schema(), copy, cfgValue.valueStat());
        new RefValidator(newCfgValue, errs).validate();
        errs.checkErrors("validate", true);
        return errs;
    }


    public RecordEditResult deleteRecord(String table, String id) {
        RecordEditResult err = checkErr(table);
        if (err != null) {
            return err;
        }

        if (id == null) {
            return ofErr(idNotSet, table);
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        TableSchema tableSchema = vTable.schema();
        ValueErrs errs = ValueErrs.of();
        Value pkValue = ValuePack.unpackTablePrimaryKey(id, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            List<String> idParseErrs = errs.errs().stream().map(Object::toString).collect(Collectors.toList());
            return new RecordEditResult(idParseErr, table, id, idParseErrs, List.of());
        }

        VStruct old = vTable.primaryKeyMap().get(pkValue);
        if (old == null) {
            return new RecordEditResult(idNotFound, table, id, List.of(), List.of());
        }

        try {
            // 最后确定其他都对的时候再存储
            boolean deleteOk = VTableJsonStore.deleteRecordStore(tableSchema, id, dataDir, cfgValue.valueStat());
            if (!deleteOk) {
                return new RecordEditResult(jsonStoreErr, table, id, List.of("delete fail"), List.of());
            }
        } catch (Exception e) {
            return new RecordEditResult(jsonStoreErr, table, id, List.of(e.getMessage()), List.of());
        }

        Map<Value, VStruct> copy = new LinkedHashMap<>(vTable.primaryKeyMap());
        copy.remove(pkValue);
        List<VStruct> newRecordList = copy.values().stream().toList();
        ValueErrs valueErrs = applyNewRecordList(newRecordList, tableSchema);
        List<String> errStrList = valueErrs.errs().stream().map(Object::toString).toList();

        List<SchemaService.RecordId> recordIds = SchemaService.getRecordIds(newCfgValue.vTableMap().get(table));
        return new RecordEditResult(deleteOk, table, id, errStrList, recordIds);
    }

}
