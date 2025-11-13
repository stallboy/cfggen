package configgen.mcpserver;

import configgen.gen.Parameter;
import configgen.gen.ParameterParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpServerTest {

    @Test
    void testMcpServerCreation() {
        // 测试MCP服务器创建
        Parameter parameter = new ParameterParser("mcpserver");
        McpServer server = new McpServer(parameter);

        assertNotNull(server);
    }

    @Test
    void testSchemaQueryWithInvalidTable() {
        // 测试无效表名的模式查询
        Parameter parameter = new ParameterParser("mcpserver");
        McpServer server = new McpServer(parameter);

        String result = server.schema_query("nonexistent_table");
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Table not found"));
    }

    @Test
    void testDataQueryWithInvalidTable() {
        // 测试无效表名的数据查询
        Parameter parameter = new ParameterParser("mcpserver");
        McpServer server = new McpServer(parameter);

        String result = server.data_query("nonexistent_table", "");
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Table not found"));
    }

    @Test
    void testDataUpdateWithInvalidParameters() {
        // 测试无效参数的更新操作
        Parameter parameter = new ParameterParser("mcpserver");
        McpServer server = new McpServer(parameter);

        String result = server.data_update("", "", "");
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Table name is required"));
    }

    @Test
    void testDataRemoveWithInvalidParameters() {
        // 测试无效参数的删除操作
        Parameter parameter = new ParameterParser("mcpserver");
        McpServer server = new McpServer(parameter);

        String result = server.data_remove("", "");
        assertTrue(result.contains("error"));
        assertTrue(result.contains("Table name is required"));
    }
}