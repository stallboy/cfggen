package configgen.mcpserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import configgen.mcpserver.McpConstants;
import configgen.mcpserver.services.ResponseFormatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 工具列表处理器
 */
public class ToolsListHandler extends BaseHandler {

    public ToolsListHandler(ResponseFormatter responseFormatter) {
        super(responseFormatter);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!McpConstants.HTTP_GET.equals(exchange.getRequestMethod())) {
            responseFormatter.sendError(exchange, 405, "Method not allowed");
            return;
        }

        List<Map<String, Object>> tools = List.of(
            createTool("schema_query", "查询表结构信息",
                Map.of("table", Map.of("type", "string", "description", "表名"))),
            createTool("data_query", "查询表数据",
                Map.of(
                    "table", Map.of("type", "string", "description", "表名"),
                    "condition", Map.of("type", "string", "description", "查询条件"))),
            createTool("data_update", "更新表数据",
                Map.of(
                    "table", Map.of("type", "string", "description", "表名"),
                    "id", Map.of("type", "string", "description", "记录ID"),
                    "newValue", Map.of("type", "string", "description", "新值（JSON格式）"))),
            createTool("data_remove", "删除表数据",
                Map.of(
                    "table", Map.of("type", "string", "description", "表名"),
                    "id", Map.of("type", "string", "description", "记录ID")))
        );

        Map<String, Object> response = Map.of("tools", tools);
        responseFormatter.sendJsonResponse(exchange, response);
    }

    private Map<String, Object> createTool(String name, String description, Map<String, Object> inputSchema) {
        return Map.of(
            "name", name,
            "description", description,
            "inputSchema", Map.of("type", "object", "properties", inputSchema)
        );
    }
}