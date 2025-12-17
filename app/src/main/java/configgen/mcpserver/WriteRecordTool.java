package configgen.mcpserver;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.schema.TableSchema;
import configgen.value.*;
import configgen.write.AddOrUpdateService;
import configgen.write.DeleteService;
import configgen.write.VTableJsonStorage;
import configgen.write.VTableStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("unused")
public class WriteRecordTool {


    public record AddOrUpdateRecordResult(AddOrUpdateService.AddOrUpdateErrorCode errorCode,
                                          String table,
                                          String recordId,
                                          List<String> errorMessages) implements McpStructuredContent {
    }

    private static final Object lock = new Object();

    @McpTool(description = "add or update record")
    public AddOrUpdateRecordResult addOrUpdateRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                                                  String tableName,
                                                                  @McpToolParam(name = "recordJsonStr", description = "record json string", required = true)
                                                                  String recordJsonStr) {

        synchronized (lock) {
            CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
            Context context = vc.context();
            CfgValue cfgValue = vc.cfgValue();

            AddOrUpdateService.AddOrUpdateRecordResult ar = AddOrUpdateService.addOrUpdateRecord(context, cfgValue, tableName, recordJsonStr);
            if (ar.newCfgValue() != null) {
                CfgMcpServer.getInstance().updateCfgValue(ar.newCfgValue());
            }

            return new AddOrUpdateRecordResult(ar.errorCode(), tableName, ar.recordId(), ar.errorMessages());
        }

    }

    public record DeleteRecordResult(DeleteService.DeleteErrorCode errorCode,
                                     String table,
                                     String recordId,
                                     List<String> errorMessages) implements McpStructuredContent {
    }

    @McpTool(description = "delete record")
    public DeleteRecordResult deleteRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                                        String tableName,
                                           @McpToolParam(name = "recordId", description = "record id", required = true)
                                                        String recordId) {
        synchronized (lock) {
            CfgMcpServer.CfgValueWithContext vc = CfgMcpServer.getInstance().cfgValueWithContext();
            Context context = vc.context();
            CfgValue cfgValue = vc.cfgValue();

            DeleteService.DeleteRecordResult dr = DeleteService.deleteRecord(context, cfgValue, tableName, recordId);
            if (dr.newCfgValue() != null) {
                CfgMcpServer.getInstance().updateCfgValue(dr.newCfgValue());
            }
            return new DeleteRecordResult(dr.errorCode(), tableName, recordId, dr.errorMessages());
        }
    }
}
