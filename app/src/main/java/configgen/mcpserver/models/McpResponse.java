package configgen.mcpserver.models;

import java.util.Map;

/**
 * MCP响应模型
 */
public class McpResponse {
    private final int code;
    private final String message;
    private final Object data;
    private final String requestId;

    public McpResponse(int code, String message, Object data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public String getRequestId() {
        return requestId;
    }

    public Map<String, Object> toMap() {
        return Map.of(
            "request_id", requestId,
            "code", code,
            "message", message,
            "data", data != null ? data : Map.of()
        );
    }

    public static McpResponse success(Object data, String requestId) {
        return new McpResponse(200, "success", data, requestId);
    }

    public static McpResponse error(int code, String message, String requestId) {
        return new McpResponse(code, message, Map.of(), requestId);
    }

    public static McpResponse error(int code, String message, String requestId, Object data) {
        return new McpResponse(code, message, data, requestId);
    }
}