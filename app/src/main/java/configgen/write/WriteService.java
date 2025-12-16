package configgen.write;

import configgen.ctx.Context;
import configgen.ctx.DirectoryStructure.JsonFileInfo;
import configgen.data.CfgData.DRawSheet;
import configgen.data.CfgData.DTable;
import configgen.schema.TableSchema;
import configgen.value.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        CfgValueStat newCfgValueStat;
        try {
            if (vTable.schema().isJson()) {
                Path jsonPath = VTableJsonStorage.addOrUpdateRecord(thisValue, tableName, id,
                        context.sourceStructure().getRootDir());

                JsonFileInfo jf = context.sourceStructure().addJsonFile(tableName, jsonPath);
                newCfgValueStat = cfgValue.valueStat().newAddLastModified(tableName, id, jf.lastModified());


            } else {
                DTable dTable = context.cfgData().getDTable(tableName);
                DRawSheet sheet = VTableStorage.addOrUpdateRecord(context, vTable, dTable, pkValue, thisValue);

            }


        } catch (IOException e) {
            return new AddOrUpdateRecordResult(AddOrUpdateErrorCode.IOException, tableName, id,
                    List.of(e.getMessage()));
        }

        return null;
    }

    record NewCfgValueResult(CfgValue newCfgValue,
                             List<String> errStrList) {
    }

    private NewCfgValueResult updateDTableSheet(TableSchema tableSchema,
                                            DTable dTable,


                                            CfgValueStat newCfgValueStat) {


        CfgValueErrs errs = CfgValueErrs.of();
        VTableParser parser = new VTableParser(tableSchema, dTable, tableSchema,
                context.contextCfg().headRow(), errs);
        CfgValue.VTable newVTable = parser.parseTable();
        TextValue.setTranslatedForTable(newVTable, context.nullableLangTextFinder());

        Map<String, CfgValue.VTable> copy = new LinkedHashMap<>(cfgValue.vTableMap());
        copy.put(newVTable.name(), newVTable);
        CfgValue newCfgValue = new CfgValue(cfgValue.schema(), copy, newCfgValueStat);
        new RefValidator(newCfgValue, errs).validate();
        errs.checkErrors("validate", true);
        List<String> errStrList = errs.errs().stream().map(Object::toString).toList();

        return new NewCfgValueResult(newCfgValue, errStrList);
    }

}
