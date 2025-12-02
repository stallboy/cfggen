package configgen.mcpserver;

import com.alibaba.fastjson2.JSONObject;
import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import configgen.genbyai.TableRelatedInfoFinder;
import configgen.genbyai.TableRelatedInfoFinder.TableRecordList;
import configgen.value.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ReadRecordTool {

    @McpTool(description = "list table record")
    public String listTableRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                  String tableName,
                                  @McpToolParam(name = "extraFields", description = "extra fields to show")
                                  List<String> extraFields) {

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return "table=%s not found".formatted(tableName);
        }

        TableRecordList list = TableRelatedInfoFinder.getTableRecordListInCsv(vTable, extraFields);
        return list.prompt();
    }

    @McpTool(description = "read one record")
    public String readRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                             String tableName,
                             @McpToolParam(name = "recordId", description = "record id", required = true)
                             String recordId) {

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        CfgValue cfgValue = vc.cfgValue();

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return "table=%s not found".formatted(tableName);
        }

        CfgValueErrs errs = CfgValueErrs.of();
        CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(recordId, vTable.schema(), errs);

        if (!errs.errs().isEmpty()) {
            return "recordId=%s parse error: %s".formatted(recordId, errs.errs().stream()
                    .map(CfgValueErrs.VErr::toString)
                    .collect(Collectors.joining("; ")));
        }

        CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
        if (vRecord == null) {
            return "recordId=%s not found".formatted(recordId);
        }

        Map<ValueRefCollector.RefId, CfgValue.VStruct> frontier = new LinkedHashMap<>();
        JSONObject object = new ValueToJson(cfgValue, frontier).toJson(vRecord);
        return object.toJSONString();
    }
}
