package configgen.editorserver;

import configgen.ctx.Context;
import configgen.ctx.DirectoryStructure;
import configgen.data.CfgData;
import configgen.schema.Msg;
import configgen.schema.TableSchema;
import configgen.util.Logger;
import configgen.value.*;
import configgen.write.VTableJsonStorage;
import configgen.write.VTableStorage;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static configgen.editorserver.RecordEditService.ResultCode.*;
import static configgen.value.CfgValue.*;

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
                                                          String jsonStr) {
        RecordEditResult err = checkErr(cfgValue, table);
        if (err != null) {
            return new ResultWithNewCfgValue(err, cfgValue);
        }

        Logger.log("addOrUpdateRecord %s", jsonStr);
        VTable vTable = cfgValue.vTableMap().get(table);
        TableSchema tableSchema = vTable.schema();
        CfgValueErrs parseErrs = CfgValueErrs.of();
        VStruct thisValue = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(jsonStr);
        parseErrs.checkErrors("check json", true, true);
        if (!parseErrs.errs().isEmpty()) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(jsonParseErr, table, "",
                            parseErrs.errs().stream().map(Msg::msg).toList()),
                    cfgValue);
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

        CfgValueStat newCfgValueStat = cfgValue.valueStat();
        try {
            // 最后确定其他都对的时候再存储
            if (vTable.schema().isJson()) {
                Path writePath = VTableJsonStorage.addOrUpdateRecord(thisValue, table, id,
                        context.sourceStructure().getRootDir());
                DirectoryStructure.JsonFileInfo jf = context.sourceStructure().addJsonFile(table, writePath);
                newCfgValueStat = newCfgValueStat.newAddLastModified(table, id, jf.lastModified());
            } else {
                CfgData.DTable dTable = context.cfgData().getDTable(table);
                VTableStorage.addOrUpdateRecord(context, vTable, dTable, pkValue, thisValue);
            }
        } catch (Exception e) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(storeErr, table, id, List.of(e.getMessage())),
                    cfgValue);
        }


        return applyNewRecords(context, cfgValue,
                tableSchema, id,
                newRecordList, newCfgValueStat,
                code);
    }

    private static ResultWithNewCfgValue applyNewRecords(Context context,
                                                         CfgValue cfgValue,
                                                         TableSchema tableSchema,
                                                         String id,
                                                         List<VStruct> newRecordList,
                                                         CfgValueStat newCfgValueStat,
                                                         ResultCode code) {
        CfgValueErrs errs = CfgValueErrs.of();
        VTableCreator creator = new VTableCreator(tableSchema, errs);
        VTable newVTable = creator.create(newRecordList);
        TextValue.setTranslatedForTable(newVTable, context.nullableLangTextFinder());

        Map<String, VTable> copy = new LinkedHashMap<>(cfgValue.vTableMap());
        copy.put(newVTable.name(), newVTable);
        CfgValue newCfgValue = new CfgValue(cfgValue.schema(), copy, newCfgValueStat);
        new RefValidator(newCfgValue, errs).validate();
        errs.checkErrors("validate", true);
        List<String> errStrList = errs.errs().stream().map(Object::toString).toList();

        return new ResultWithNewCfgValue(
                new RecordEditResult(code, tableSchema.name(), id, errStrList),
                newCfgValue);
    }

    private static RecordEditResult checkErr(CfgValue cfgValue, String table) {
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
        return null;
    }

    private static RecordEditResult ofErr(ResultCode code, String table) {
        return new RecordEditResult(code, table == null ? "" : table, "", List.of());
    }


    public static ResultWithNewCfgValue deleteRecord(@NotNull Context context,
                                                     @NotNull CfgValue cfgValue,
                                                     String table,
                                                     String id) {
        var err = checkErr(cfgValue, table);
        if (err != null) {
            return new ResultWithNewCfgValue(err, cfgValue);
        }

        if (id == null) {
            return new ResultWithNewCfgValue(
                    ofErr(idNotSet, table),
                    cfgValue);
        }

        VTable vTable = cfgValue.vTableMap().get(table);
        TableSchema tableSchema = vTable.schema();
        CfgValueErrs errs = CfgValueErrs.of();
        Value pkValue = ValuePack.unpackTablePrimaryKey(id, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            List<String> idParseErrs = errs.errs().stream().map(Object::toString).collect(Collectors.toList());
            return new ResultWithNewCfgValue(
                    new RecordEditResult(idParseErr, table, id, idParseErrs),
                    cfgValue);
        }

        VStruct old = vTable.primaryKeyMap().get(pkValue);
        if (old == null) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(idNotFound, table, id, List.of()),
                    cfgValue);
        }

        CfgValueStat newCfgValueStat = cfgValue.valueStat();
        try {
            if (vTable.schema().isJson()) {
                // 最后确定其他都对的时候再存储
                Path jsonPath = VTableJsonStorage.deleteRecord(table, id,
                        context.sourceStructure().getRootDir());
                context.sourceStructure().removeJsonFile(table, jsonPath);
                newCfgValueStat = newCfgValueStat.newRemoveLastModified(table, id);

            } else {
                CfgData.DTable dTable = context.cfgData().getDTable(table);
                VTableStorage.deleteRecord(context, dTable, old);
            }
        } catch (Exception e) {
            return new ResultWithNewCfgValue(
                    new RecordEditResult(storeErr, table, id, List.of(e.getMessage())),
                    cfgValue);
        }


        Map<Value, VStruct> copy = new LinkedHashMap<>(vTable.primaryKeyMap());
        copy.remove(pkValue);
        List<VStruct> newRecordList = copy.values().stream().toList();

        return applyNewRecords(context, cfgValue,
                tableSchema, id,
                newRecordList, newCfgValueStat,
                deleteOk);
    }

}
