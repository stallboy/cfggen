package configgen.mcpserver.tools;

import configgen.mcpserver.McpConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具注册表
 */
public class ToolRegistry {
    private final Map<String, McpTool> tools = new HashMap<>();

    public ToolRegistry() {
        // 工具将在外部注册
    }

    /**
     * 注册工具
     * @param toolName 工具名称
     * @param tool 工具实例
     */
    public void registerTool(String toolName, McpTool tool) {
        tools.put(toolName, tool);
    }

    /**
     * 获取工具
     * @param toolName 工具名称
     * @return 工具实例，如果不存在返回null
     */
    public McpTool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * 检查工具是否存在
     * @param toolName 工具名称
     * @return 是否存在
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }
}