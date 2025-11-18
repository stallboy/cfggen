package configgen.mcpserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import configgen.mcpserver.McpConstants;
import configgen.mcpserver.services.ResponseFormatter;
import configgen.value.CfgValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资源列表处理器
 */
public class ResourcesListHandler extends BaseHandler {
    private final CfgValue cfgValue;

    public ResourcesListHandler(ResponseFormatter responseFormatter, CfgValue cfgValue) {
        super(responseFormatter);
        this.cfgValue = cfgValue;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!McpConstants.HTTP_GET.equals(exchange.getRequestMethod())) {
            responseFormatter.sendError(exchange, 405, "Method not allowed");
            return;
        }

        List<Map<String, Object>> resources = new ArrayList<>();
        for (String tableName : cfgValue.vTableMap().keySet()) {
            resources.add(Map.of(
                "uri", "table://" + tableName,
                "name", tableName,
                "description", "Table: " + tableName,
                "mimeType", "application/json"
            ));
        }

        responseFormatter.sendJsonResponse(exchange, Map.of("resources", resources));
    }
}