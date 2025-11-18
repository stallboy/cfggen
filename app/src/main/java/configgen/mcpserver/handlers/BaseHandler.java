package configgen.mcpserver.handlers;

import configgen.mcpserver.services.ResponseFormatter;

/**
 * 处理器基类
 */
public abstract class BaseHandler implements McpHandler {
    protected final ResponseFormatter responseFormatter;

    protected BaseHandler(ResponseFormatter responseFormatter) {
        this.responseFormatter = responseFormatter;
    }
}