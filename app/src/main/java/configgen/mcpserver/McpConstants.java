package configgen.mcpserver;

/**
 * MCP服务器常量定义
 */
public final class McpConstants {

    // MCP 标准端点
    public static final String INITIALIZE_PATH = "/initialize";
    public static final String HEALTH_PATH = "/health";
    public static final String TOOLS_LIST_PATH = "/tools/list";
    public static final String TOOLS_CALL_PATH = "/tools/call";
    public static final String RESOURCES_LIST_PATH = "/resources/list";
    public static final String RESOURCES_READ_PATH = "/resources/read";

    // MCP 协议版本
    public static final String MCP_PROTOCOL_VERSION = "2024-11-05";
    public static final String SERVER_VERSION = "1.0.0";

    // 工具名称
    public static final String TOOL_SCHEMA_QUERY = "schema_query";
    public static final String TOOL_DATA_QUERY = "data_query";
    public static final String TOOL_DATA_UPDATE = "data_update";
    public static final String TOOL_DATA_REMOVE = "data_remove";

    // 资源URI前缀
    public static final String RESOURCE_URI_PREFIX = "table://";

    // HTTP方法
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";

    // 默认配置
    public static final int DEFAULT_PORT = 3000;
    public static final int DEFAULT_WAIT_SECONDS = 0;
    public static final int DEFAULT_QUERY_LIMIT = 100;

    private McpConstants() {
        // 防止实例化
    }
}