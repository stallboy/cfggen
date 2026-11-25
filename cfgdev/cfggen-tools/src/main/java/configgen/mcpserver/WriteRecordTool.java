package configgen.mcpserver;

import org.jspecify.annotations.Nullable;

import com.github.codeboyzhou.mcp.declarative.annotation.McpTool;
import com.github.codeboyzhou.mcp.declarative.annotation.McpToolParam;
import com.github.codeboyzhou.mcp.declarative.server.McpStructuredContent;
import configgen.gen.WatchAndPostRun;
import configgen.ctx.StateCoordinator.EditOutcome;
import configgen.write.AddOrUpdateService;
import configgen.write.DeleteService;

import java.util.List;

@SuppressWarnings("unused")
public class WriteRecordTool {


    public record AddOrUpdateRecordResult(AddOrUpdateService.AddOrUpdateErrorCode errorCode,
                                          String table,
                                          @Nullable String recordId,
                                          List<String> errorMessages) implements McpStructuredContent {
    }

    @McpTool(description = "add or update record")
    public AddOrUpdateRecordResult addOrUpdateRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                                                  String tableName,
                                                                  @McpToolParam(name = "recordJsonStr", description = "record json string", required = true)
                                                                  String recordJsonStr) {

        // 统一编辑临界区：与reload换代、其他写操作（含同进程EditorServer的写接口）互斥；
        // 编辑完成后锁内统一刷新所有server的内存快照（本server的cfgValueWithContext由回调重建，无需手动update）
        return WatchAndPostRun.INSTANCE.runEdit(ctx -> {
            var value = CfgMcpServer.getInstance().makeValue(ctx);

            AddOrUpdateService.AddOrUpdateRecordResult ar =
                    AddOrUpdateService.addOrUpdateRecord(ctx, value, tableName, recordJsonStr);

            return new EditOutcome<>(
                    new AddOrUpdateRecordResult(ar.errorCode(), tableName, ar.recordId(), ar.errorMessages()),
                    ar.committed());
        });
    }

    public record DeleteRecordResult(DeleteService.DeleteErrorCode errorCode,
                                     String table,
                                     @Nullable String recordId,
                                     List<String> errorMessages) implements McpStructuredContent {
    }

    @McpTool(description = "delete record")
    public DeleteRecordResult deleteRecord(@McpToolParam(name = "table", description = "table full name", required = true)
                                                        String tableName,
                                           @McpToolParam(name = "recordId", description = "record id", required = true)
                                                        String recordId) {
        return WatchAndPostRun.INSTANCE.runEdit(ctx -> {
            var value = CfgMcpServer.getInstance().makeValue(ctx);

            DeleteService.DeleteRecordResult dr =
                    DeleteService.deleteRecord(ctx, value, tableName, recordId);

            return new EditOutcome<>(
                    new DeleteRecordResult(dr.errorCode(), tableName, recordId, dr.errorMessages()),
                    dr.committed());
        });
    }
}
