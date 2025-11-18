package configgen.mcpserver.handlers;

import com.sun.net.httpserver.HttpExchange;
import configgen.mcpserver.McpConstants;
import configgen.mcpserver.services.ResponseFormatter;

import java.io.IOException;
import java.util.Map;

/**
 * 初始化处理器
 */
public class InitializeHandler extends BaseHandler {

    public InitializeHandler(ResponseFormatter responseFormatter) {
        super(responseFormatter);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!McpConstants.HTTP_GET.equals(exchange.getRequestMethod())) {
            responseFormatter.sendError(exchange, 405, "Method not allowed");
            return;
        }

        Map<String, Object> response = Map.of(
            "protocolVersion", McpConstants.MCP_PROTOCOL_VERSION,
            "serverVersion", McpConstants.SERVER_VERSION,
            "capabilities", Map.of(
                "tools", Map.of(
                    "listChanged", false,
                    "subscribe", false
                ),
                "resources", Map.of(
                    "listChanged", false,
                    "subscribe", false
                )
            ),
            "serverInfo", Map.of(
                "name", "cfggen-mcp-server",
                "version", McpConstants.SERVER_VERSION
            )
        );

        responseFormatter.sendJsonResponse(exchange, response);
    }
}