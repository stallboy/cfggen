package configgen.mcpserver.handlers;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * MCP处理器接口
 */
public interface McpHandler {

    /**
     * 处理HTTP请求
     * @param exchange HTTP交换对象
     * @throws IOException IO异常
     */
    void handle(HttpExchange exchange) throws IOException;
}