package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import configgen.ctx.Context;
import configgen.schema.TableSchema;
import configgen.value.*;

import java.nio.file.Path;
import java.util.stream.Collectors;

public class WriteRecordTool {

    @McpTool(description = "add or update record")
    public String addOrUpdateRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                    String tableName,
                                    @McpToolParam(name = "recordJsonStr", description = "record json string", required = true)
                                    String recordJsonStr) {

        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        Context context = vc.context();
        CfgValue cfgValue = vc.cfgValue();

        CfgValue.VTable vTable = cfgValue.getTable(tableName);
        if (vTable == null) {
            return "table=%s not found".formatted(tableName);
        }

        TableSchema tableSchema = vTable.schema();
        CfgValueErrs parseErrs = CfgValueErrs.of();
        CfgValue.VStruct thisValue = new ValueJsonParser(vTable.schema(), parseErrs).fromJson(recordJsonStr);
        parseErrs.checkErrors("check json", true, true);
        if (!parseErrs.errs().isEmpty()) {
            return "record parse error: %s".formatted(parseErrs.errs().stream()
                    .map(CfgValueErrs.VErr::toString)
                    .collect(Collectors.joining("; ")));
        }

        CfgValue.Value pkValue = ValueUtil.extractPrimaryKeyValue(thisValue, tableSchema);
        String id = pkValue.packStr();

        if (vTable.schema().isJson()) {
            Path writePath;
            try {
                // 最后确定其他都对的时候再存储
                writePath = VTableJsonStore.addOrUpdateRecordStore(thisValue, tableName, id,
                        context.getSourceStructure().getRootDir());
            } catch (Exception e) {
                return "record store error: %s".formatted(e.getMessage());
            }

            return "record stored success at %s".formatted(writePath.toString());
        } else {
            boolean ok = VTableWriter.addOrUpdateRecord(cfgValue, vTable, pkValue, thisValue);
            return ok ? "record stored success" : "record store failed";
        }
    }

    @McpTool(description = "delete record")
    public String deleteRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                               String tableName,
                               @McpToolParam(name = "recordId", description = "record id", required = true)
                               String recordId) {
        CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
        Context context = vc.context();
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

        CfgValue.VStruct old = vTable.primaryKeyMap().get(pkValue);
        if (old == null) {
            return "record not found by id=%s".formatted(recordId);
        }
        if (vTable.schema().isJson()) {
            Path jsonPath;
            try {
                // 最后确定其他都对的时候再存储
                jsonPath = VTableJsonStore.deleteRecordStore(tableName, recordId,
                        context.getSourceStructure().getRootDir());
                if (jsonPath == null) {
                    return "delete record file failed";
                }
            } catch (Exception e) {
                return "record delete error: %s".formatted(e.getMessage());
            }
            return "record deleted success at %s".formatted(jsonPath.toString());
        } else {
            boolean ok = VTableWriter.deleteRecord(cfgValue, vTable, pkValue);
            return ok ? "record deleted success" : "record delete failed";
        }
    }
}
