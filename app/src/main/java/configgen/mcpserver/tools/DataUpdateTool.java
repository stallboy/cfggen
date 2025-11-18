package configgen.mcpserver.tools;

import configgen.mcpserver.models.McpRequest;
import configgen.mcpserver.models.McpResponse;
import configgen.mcpserver.services.DataUpdateService;

import java.util.Map;

/**
 * 数据更新工具
 */
public class DataUpdateTool implements McpTool {
    private final DataUpdateService dataUpdateService;

    public DataUpdateTool(DataUpdateService dataUpdateService) {
        this.dataUpdateService = dataUpdateService;
    }

    @Override
    public McpResponse execute(McpRequest request) {
        String table = request.getArguments().getString("table");
        String id = request.getArguments().getString("id");
        String newValue = request.getArguments().getString("newValue");
        String requestId = request.getRequestId();

        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId);
        }

        if (id == null || id.isEmpty()) {
            return McpResponse.error(400, "Record ID is required", requestId);
        }

        if (newValue == null || newValue.isEmpty()) {
            return McpResponse.error(400, "New value is required", requestId);
        }

        try {
            Map<String, Object> result = dataUpdateService.updateData(table, id, newValue, requestId);
            return McpResponse.success(result, requestId);
        } catch (Exception e) {
            return McpResponse.error(500, "Error updating data: " + e.getMessage(), requestId);
        }
    }
}