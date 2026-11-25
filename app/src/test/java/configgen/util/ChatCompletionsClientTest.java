package configgen.util;

import com.alibaba.fastjson2.JSON;
import com.sun.net.httpserver.HttpServer;
import configgen.util.ChatCompletionsClient.Config;
import configgen.util.ChatCompletionsClient.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChatCompletionsClientTest {
    @Test
    void omittedOrNullBaseUrlUsesDefault(@TempDir Path tempDir) throws IOException {
        var path = tempDir.resolve("ai.json");
        Files.writeString(path, """
                {"apiKey":"test-key","model":"test-model"}
                """);
        assertEquals("https://api.openai.com", Config.readFromFile(path.toString()).baseUrl());

        Files.writeString(path, """
                {"baseUrl":null,"apiKey":"test-key","model":"test-model"}
                """);
        assertEquals("https://api.openai.com", Config.readFromFile(path.toString()).baseUrl());
    }

    @Test
    void sendsMessagesToLegacyBaseUrlAndRetriesTransientFailure() throws IOException {
        var attempts = new AtomicInteger();
        var authorization = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/proxy/v1/chat/completions", exchange -> {
                try (exchange) {
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    int attempt = attempts.incrementAndGet();
                    if (attempt < 3) {
                        exchange.sendResponseHeaders(attempt == 1 ? 429 : 503, -1);
                        return;
                    }
                    byte[] response = """
                            {"choices":[{"message":{"content":"你好"}}]}
                            """.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                }
            });
            server.start();
            var client = new ChatCompletionsClient(
                    new Config("http://127.0.0.1:" + server.getAddress().getPort() + "/proxy", "test-key", "test-model"));

            var completion = client.complete(List.of(new Message("user", "早上好")));

            assertEquals("你好", completion.content());
            assertNull(completion.usage());
            assertEquals(3, attempts.get());
            assertEquals("Bearer test-key", authorization.get());
            var request = JSON.parseObject(body.get());
            assertEquals("test-model", request.getString("model"));
            assertEquals(0.0, request.getDoubleValue("temperature"));
            var message = request.getJSONArray("messages").getJSONObject(0);
            assertEquals("user", message.getString("role"));
            assertEquals("早上好", message.getString("content"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void authenticationFailureIsReportedWithoutRetrying() throws IOException {
        var attempts = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/chat/completions", exchange -> {
                try (exchange) {
                    attempts.incrementAndGet();
                    exchange.sendResponseHeaders(401, -1);
                }
            });
            server.start();
            var client = new ChatCompletionsClient(new Config(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions", "test-key", "test-model"));

            var error = assertThrows(IllegalStateException.class,
                    () -> client.complete(List.of(new Message("user", "test"))));

            assertEquals("Chat completion request failed with HTTP 401", error.getMessage());
            assertEquals(1, attempts.get());
        } finally {
            server.stop(0);
        }
    }
}
