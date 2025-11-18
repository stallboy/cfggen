package configgen.mcpserver.tools;

import configgen.mcpserver.models.McpRequest;
import configgen.mcpserver.models.McpResponse;

/**
 * MCP工具接口
 */
public interface McpTool {

    /**
     * 执行工具操作
     * @param request MCP请求
     * @return MCP响应
     */
    McpResponse execute(McpRequest request);
}