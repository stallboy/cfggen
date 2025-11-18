package configgen.mcpserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import configgen.mcpserver.McpConstants;
import configgen.mcpserver.services.ResponseFormatter;
import configgen.value.CfgValue;

import java.io.IOException;
import java.util.Map;

/**
 * 健康检查处理器
 */
public class HealthHandler extends BaseHandler {
    private final CfgValue cfgValue;
    private final long startTime;

    public HealthHandler(ResponseFormatter responseFormatter, CfgValue cfgValue, long startTime) {
        super(responseFormatter);
        this.cfgValue = cfgValue;
        this.startTime = startTime;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!McpConstants.HTTP_GET.equals(exchange.getRequestMethod())) {
            responseFormatter.sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> response = Map.of(
            "status", "healthy",
            "timestamp", System.currentTimeMillis(),
            "uptime", System.currentTimeMillis() - startTime,
            "tables", cfgValue.vTableMap().size(),
            "memory", Map.of(
                "used", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
                "total", Runtime.getRuntime().totalMemory()
            )
        );

        responseFormatter.sendJsonResponse(exchange, response);
    }
}