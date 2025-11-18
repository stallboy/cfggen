package configgen.mcpserver.handlers;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import configgen.mcpserver.McpConstants;
import configgen.mcpserver.models.McpRequest;
import configgen.mcpserver.models.McpResponse;
import configgen.mcpserver.services.ResponseFormatter;
import configgen.mcpserver.tools.McpTool;
import configgen.mcpserver.tools.ToolRegistry;
import configgen.util.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工具调用处理器
 */
public class ToolsCallHandler extends BaseHandler {
    private final ToolRegistry toolRegistry;

    public ToolsCallHandler(ResponseFormatter responseFormatter, ToolRegistry toolRegistry) {
        super(responseFormatter);
        this.toolRegistry = toolRegistry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!McpConstants.HTTP_POST.equals(exchange.getRequestMethod())) {
            responseFormatter.sendError(exchange, 405, "Method not allowed");
            return;
        }

        String requestId = generateRequestId();

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (requestBody.isEmpty()) {
                sendErrorWithRequestId(exchange, 400, "Request body is empty", requestId);
                return;
            }

            JSONObject request = JSON.parseObject(requestBody);
            if (request == null) {
                sendErrorWithRequestId(exchange, 400, "Invalid JSON request", requestId);
                return;
            }

            String toolName = request.getString("name");
            if (toolName == null || toolName.isEmpty()) {
                sendErrorWithRequestId(exchange, 400, "Tool name is required", requestId);
                return;
            }

            JSONObject arguments = request.getJSONObject("arguments");
            if (arguments == null) {
                sendErrorWithRequestId(exchange, 400, "Arguments are required", requestId);
                return;
            }

            McpRequest mcpRequest = McpRequest.fromJson(request, requestId);
            McpTool tool = toolRegistry.getTool(toolName);
            if (tool == null) {
                sendErrorWithRequestId(exchange, 400, "Unknown tool: " + toolName, requestId);
                return;
            }

            McpResponse result = tool.execute(mcpRequest);
            sendJsonResponse(exchange, Map.of("content", List.of(
                Map.of("type", "text", "text", JSON.toJSONString(result.toMap()))
            )));

        } catch (Exception e) {
            Logger.log("Error handling tool call: " + e.getMessage());
            sendErrorWithRequestId(exchange, 500, "Internal server error: " + e.getMessage(), requestId);
        }
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    private void sendErrorWithRequestId(HttpExchange exchange, int code, String message, String requestId) throws IOException {
        McpResponse errorResponse = McpResponse.error(code, message, requestId);
        responseFormatter.sendJsonResponse(exchange, errorResponse.toMap(), code);
    }

    private void sendJsonResponse(HttpExchange exchange, Object response) throws IOException {
        responseFormatter.sendJsonResponse(exchange, response);
    }
}