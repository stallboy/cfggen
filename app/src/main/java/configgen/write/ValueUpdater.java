package configgen.write;

import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.data.DataUpdater;
import configgen.data.DataUpdater.NewCfgDataResult;
import configgen.data.Source;
import configgen.schema.CfgSchema;
import configgen.value.*;
import configgen.value.CfgValue.VStruct;
import configgen.value.CfgValue.VTable;
import configgen.value.CfgValue.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ValueUpdater {


    public record NewCfgValueResult(@NotNull CfgValue newCfgValue,
                                    @NotNull CfgData newCfgData,
                                    @NotNull List<String> errStrList) {
    }

    public static NewCfgValueResult updateByReloadTableData(@NotNull Context context,
                                                            @NotNull CfgValue cfgValue,
                                                            @NotNull VTable vTable) {

        CfgSchema schema = cfgValue.schema();
        if (schema.isPartial()) {
            throw new IllegalArgumentException("update only supports full value");
        }
        CfgData.DTable dTable = context.cfgData().getDTable(vTable.name());
        if (dTable == null) {
            throw new IllegalArgumentException("DTable not found: " + vTable.name());
        }

        NewCfgDataResult newDataResult = DataUpdater.updateByReloadTable(context, dTable);
        CfgData newCfgData = newDataResult.newCfgData();
        CfgValueErrs errs = CfgValueErrs.of();
        VTableParser parser = new VTableParser(vTable.schema(), newCfgData.getDTable(vTable.name()),
                vTable.schema(), context.contextCfg().headRow(), errs);
        VTable newVTable = parser.parseTable();
        TextValue.setTranslatedForTable(newVTable, context.nullableLangTextFinder());

        Map<String, VTable> newTables = new LinkedHashMap<>(cfgValue.vTableMap());
        newTables.put(newVTable.name(), newVTable);
        CfgValue newCfgValue = new CfgValue(schema, newTables, cfgValue.valueStat());
        new RefValidator(newCfgValue, errs).validate();
        errs.checkErrors("validate", true);

        List<String> errStrList = new ArrayList<>(newDataResult.errStrList());
        errStrList.addAll(errs.errs().stream().map(Object::toString).toList());
        return new NewCfgValueResult(newCfgValue, newCfgData, errStrList);
    }


    public static NewCfgValueResult updateByJsonFileAddOrUpdate(@NotNull Context context,
                                                                @NotNull CfgValue cfgValue,
                                                                @NotNull VTable vTable,
                                                                @NotNull Path relativeJsonPath) {
        CfgSchema schema = cfgValue.schema();
        if (schema.isPartial()) {
            throw new IllegalArgumentException("update only supports full value");
        }

        Path path = context.rootDir().resolve(relativeJsonPath).toAbsolutePath().normalize();
        String jsonStr;
        try {
            jsonStr = Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        CfgValueErrs parseErrs = CfgValueErrs.of();
        CfgValue.VStruct vStruct = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(jsonStr,
                Source.DFile.of(relativeJsonPath.toString(), vTable.name()));
        Value pkValue = ValueUtil.extractPrimaryKeyValue(vStruct, vTable.schema());
        String id = pkValue.packStr();
        Map<Value, VStruct> newPrimaryKeyMap = new LinkedHashMap<>(vTable.primaryKeyMap());
        newPrimaryKeyMap.put(pkValue, vStruct);
        List<VStruct> newRecordList = newPrimaryKeyMap.values().stream().toList();
        CfgValueStat newCfgValueStat = cfgValue.valueStat().newAddLastModified(vTable.name(), id,
                path.toFile().lastModified());

        return updateByNewRecords(context, cfgValue, vTable, newRecordList, newCfgValueStat);
    }

    public static NewCfgValueResult updateByJsonFileDelete(@NotNull Context context,
                                                           @NotNull CfgValue cfgValue,
                                                           @NotNull VTable vTable,
                                                           @NotNull Value pkValue,
                                                           @NotNull String id) {
        CfgSchema schema = cfgValue.schema();
        if (schema.isPartial()) {
            throw new IllegalArgumentException("update only supports full value");
        }

        Map<Value, VStruct> newPrimaryKeyMap = new LinkedHashMap<>(vTable.primaryKeyMap());
        newPrimaryKeyMap.remove(pkValue);
        List<VStruct> newRecordList = newPrimaryKeyMap.values().stream().toList();
        CfgValueStat newCfgValueStat = cfgValue.valueStat().newRemoveLastModified(vTable.name(), id);
        return updateByNewRecords(context, cfgValue, vTable, newRecordList, newCfgValueStat);
    }

    private static @NotNull NewCfgValueResult updateByNewRecords(@NotNull Context context,
                                                                 @NotNull CfgValue cfgValue,
                                                                 @NotNull VTable vTable,
                                                                 List<VStruct> newRecordList,
                                                                 CfgValueStat newCfgValueStat) {
        CfgValueErrs errs = CfgValueErrs.of();
        VTableCreator creator = new VTableCreator(vTable.schema(), errs);
        VTable newVTable = creator.create(newRecordList);
        TextValue.setTranslatedForTable(newVTable, context.nullableLangTextFinder());

        Map<String, VTable> newTables = new LinkedHashMap<>(cfgValue.vTableMap());
        newTables.put(newVTable.name(), newVTable);
        CfgValue newCfgValue = new CfgValue(cfgValue.schema(), newTables, newCfgValueStat);
        new RefValidator(newCfgValue, errs).validate();
        errs.checkErrors("validate", true);
        List<String> errStrList = errs.errs().stream().map(Object::toString).toList();

        return new NewCfgValueResult(newCfgValue, context.cfgData(), errStrList);
    }


}
