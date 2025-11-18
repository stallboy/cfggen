package configgen.mcpserver.services;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 响应格式化服务
 */
public class ResponseFormatter {

    /**
     * 发送JSON响应
     * @param exchange HTTP交换对象
     * @param response 响应数据
     * @throws IOException IO异常
     */
    public void sendJsonResponse(HttpExchange exchange, Object response) throws IOException {
        sendJsonResponse(exchange, response, 200);
    }

    /**
     * 发送JSON响应
     * @param exchange HTTP交换对象
     * @param response 响应数据
     * @param statusCode 状态码
     * @throws IOException IO异常
     */
    public void sendJsonResponse(HttpExchange exchange, Object response, int statusCode) throws IOException {
        String jsonResponse = JSON.toJSONString(response);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        exchange.sendResponseHeaders(statusCode, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 发送错误响应
     * @param exchange HTTP交换对象
     * @param code 错误码
     * @param message 错误消息
     * @throws IOException IO异常
     */
    public void sendError(HttpExchange exchange, int code, String message) throws IOException {
        sendJsonResponse(exchange, Map.of("error", message), code);
    }
}