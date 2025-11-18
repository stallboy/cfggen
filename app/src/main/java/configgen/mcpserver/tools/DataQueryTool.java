package configgen.mcpserver.tools;

import configgen.mcpserver.models.McpRequest;
import configgen.mcpserver.models.McpResponse;
import configgen.mcpserver.services.DataQueryService;

import java.util.Map;

/**
 * 数据查询工具
 */
public class DataQueryTool implements McpTool {
    private final DataQueryService dataQueryService;

    public DataQueryTool(DataQueryService dataQueryService) {
        this.dataQueryService = dataQueryService;
    }

    @Override
    public McpResponse execute(McpRequest request) {
        String table = request.getArguments().getString("table");
        String condition = request.getArguments().getString("condition");
        String requestId = request.getRequestId();

        if (table == null || table.isEmpty()) {
            return McpResponse.error(400, "Table name is required", requestId);
        }

        try {
            Map<String, Object> result = dataQueryService.queryData(table, condition, requestId);
            return McpResponse.success(result, requestId);
        } catch (Exception e) {
            return McpResponse.error(500, "Error querying data: " + e.getMessage(), requestId);
        }
    }
}