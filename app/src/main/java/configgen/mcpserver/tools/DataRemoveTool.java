package configgen.mcpserver.tools;

import configgen.mcpserver.models.McpRequest;
import configgen.mcpserver.models.McpResponse;
import configgen.mcpserver.services.DataUpdateService;

import java.util.Map;

/**
 * 数据删除工具
 */
public class DataRemoveTool implements McpTool {
    private final DataUpdateService dataUpdateService;

    public DataRemoveTool(DataUpdateService dataUpdateService) {
        this.dataUpdateService = dataUpdateService;
    }

    @Override
    public McpResponse execute(McpRequest request) {
        String table = request.getArguments().getString("table");
        String id = request.getArguments().getString("id");
        String requestId = request.getRequestId();

        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId);
        }

        if (id == null || id.isEmpty()) {
            return McpResponse.error(400, "Record ID is required", requestId);
        }

        try {
            Map<String, Object> result = dataUpdateService.removeData(table, id, requestId);
            return McpResponse.success(result, requestId);
        } catch (Exception e) {
            return McpResponse.error(500, "Error removing data: " + e.getMessage(), requestId);
        }
    }
}