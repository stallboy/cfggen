package configgen.mcpserver.models;

import com.alibaba.fastjson2.JSONObject;

/**
 * MCP请求模型
 */
public class McpRequest {
    private final String toolName;
    private final JSONObject arguments;
    private final String requestId;

    public McpRequest(String toolName, JSONObject arguments, String requestId) {
        this.toolName = toolName;
        this.arguments = arguments;
        this.requestId = requestId;
    }

    public String getToolName() {
        return toolName;
    }

    public JSONObject getArguments() {
        return arguments;
    }

    public String getRequestId() {
        return requestId;
    }

    public static McpRequest fromJson(JSONObject jsonObject, String requestId) {
        String toolName = jsonObject.getString("name");
        JSONObject arguments = jsonObject.getJSONObject("arguments");
        return new McpRequest(toolName, arguments, requestId);
    }
}