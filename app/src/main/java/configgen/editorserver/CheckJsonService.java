package configgen.editorserver;

import configgen.schema.TableSchema;
import configgen.genbyai.ByAIGenerator;
import configgen.genbyai.PromptDefault;
import configgen.value.CfgValue;
import configgen.value.CfgValueErrs;
import configgen.value.ValueJsonParser;
import configgen.value.ValueToJson;

import static configgen.editorserver.CheckJsonService.CheckJsonResultCode.*;


public class CheckJsonService {

    public record CheckJsonResult(CheckJsonResultCode resultCode,
                                  String table,
                                  String jsonResult) {
    }

    public enum CheckJsonResultCode {
        ok,
        tableNotFound,
        JsonNotFound,
        ParseJsonError,
    }


    public static CheckJsonResult checkJson(CfgValue cfgValue, String table, String raw) {
        if (table == null || table.isEmpty()) {
            return new CheckJsonResult(tableNotFound, "", "table not found");
        }

        CfgValue.VTable vTable = cfgValue.getTable(table);
        if (vTable == null) {
            return new CheckJsonResult(tableNotFound, table, "table not found");
        }

        if (raw == null || raw.isEmpty()) {
            return new CheckJsonResult(JsonNotFound, table, "json empty");
        }

        String jsonResult = raw.stripLeading().startsWith("{") ? raw : ByAIGenerator.extractJson(raw);
        if (jsonResult == null) {
            return new CheckJsonResult(JsonNotFound, table, "json not found");
        }

        TableSchema tableSchema = vTable.schema();
        try {
            jsonResult = EditorValueJson.decode(tableSchema, jsonResult);
        } catch (IllegalArgumentException e) {
            return new CheckJsonResult(ParseJsonError, table, e.getMessage());
        }
        CfgValueErrs parseErrs = CfgValueErrs.of();
        CfgValue.VStruct record = new ValueJsonParser(tableSchema, parseErrs).fromJson(jsonResult);
        parseErrs.checkErrors("check json", true, true);

        if (!parseErrs.errs().isEmpty()) {
            String err = PromptDefault.FIX_ERROR.formatted(parseErrs.toString());
            return new CheckJsonResult(ParseJsonError, table, err);
        }

        ValueToJson serializer = ValueToJson.forEditor(null, null, null);
        serializer.setSaveDefault(false);
        String jsonString = com.alibaba.fastjson2.JSON.toJSONString(serializer.toJson(record), com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);
        return new CheckJsonResult(ok, table, jsonString);
    }

}
