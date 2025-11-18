package configgen.mcpserver.handlers;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpExchange;
import configgen.mcpserver.McpConstants;
import configgen.mcpserver.services.DataQueryService;
import configgen.mcpserver.services.ResponseFormatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 资源读取处理器
 */
public class ResourcesReadHandler extends BaseHandler {
    private final DataQueryService dataQueryService;

    public ResourcesReadHandler(ResponseFormatter responseFormatter, DataQueryService dataQueryService) {
        super(responseFormatter);
        this.dataQueryService = dataQueryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!McpConstants.HTTP_GET.equals(exchange.getRequestMethod())) {
            responseFormatter.sendError(exchange, 405, "Method not allowed");
            return;
        }

        String uri = exchange.getRequestURI().getQuery();
        if (uri == null || !uri.startsWith(McpConstants.RESOURCE_URI_PREFIX)) {
            responseFormatter.sendError(exchange, 400, "Invalid resource URI. Expected format: " + McpConstants.RESOURCE_URI_PREFIX + "{table_name}");
            return;
        }

        String tableName = uri.substring(McpConstants.RESOURCE_URI_PREFIX.length());
        if (tableName.isEmpty()) {
            responseFormatter.sendError(exchange, 400, "Table name is required in resource URI");
            return;
        }

        String requestId = generateRequestId();
        Map<String, Object> result = dataQueryService.queryData(tableName, "", requestId);

        responseFormatter.sendJsonResponse(exchange, Map.of(
            "contents", List.of(Map.of(
                "uri", uri,
                "mimeType", "application/json",
                "text", JSON.toJSONString(result)
            ))
        ));
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}