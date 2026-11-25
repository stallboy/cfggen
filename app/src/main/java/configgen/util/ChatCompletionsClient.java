package configgen.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ChatCompletionsClient {
    private static final String COMPLETIONS_PATH = "/chat/completions";
    private static final int MAX_ATTEMPTS = 3;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private final URI endpoint;
    private final String apiKey;
    private final String model;

    public ChatCompletionsClient(Config config) {
        endpoint = endpoint(config.baseUrl());
        apiKey = config.apiKey();
        model = config.model();
    }

    public record Config(String baseUrl, String apiKey, String model) {
        public Config(@Nullable String baseUrl, String apiKey, String model) {
            this.baseUrl = baseUrl == null ? "https://api.openai.com" : baseUrl;
            this.apiKey = apiKey;
            this.model = model;
        }

        public static Config readFromFile(String cfgFn) {
            Path path = Path.of(cfgFn);
            if (!Files.exists(path)) {
                throw new RuntimeException(cfgFn + " not exist!");
            }
            String jsonStr;
            try {
                jsonStr = Files.readString(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (jsonStr.isEmpty()) {
                throw new RuntimeException(cfgFn + " is empty!");
            }
            return JSON.parseObject(jsonStr, Config.class);
        }
    }

    public record Message(String role, String content) {}

    public record Completion(String content, @Nullable JSONObject usage) {}

    public Completion complete(List<Message> messages) {
        JSONObject body = JSONObject.of("model", model, "messages", messages, "temperature", 0.0);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();

        HttpResponse<String> response;
        try {
            response = sendWithRetry(request);
        } catch (IOException e) {
            throw new IllegalStateException("Chat completion request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chat completion request interrupted", e);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Chat completion request failed with HTTP " + response.statusCode());
        }
        return parseCompletion(response.body());
    }

    private static HttpResponse<String> sendWithRetry(HttpRequest request) throws IOException, InterruptedException {
        var bodyHandler = HttpResponse.BodyHandlers.ofString();
        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = HTTP_CLIENT.send(request, bodyHandler);
                if (!isRetryable(response.statusCode())) {
                    return response;
                }
            } catch (IOException ignored) {
                // Retry transport failures. The final attempt reports its failure to the caller.
            }
            long delay = 1000L << (attempt - 1);
            Thread.sleep(ThreadLocalRandom.current().nextLong(delay * 9 / 10, delay * 11 / 10 + 1));
        }
        return HTTP_CLIENT.send(request, bodyHandler);
    }

    private static boolean isRetryable(int status) {
        return switch (status) {
            case 408, 409, 429 -> true;
            default -> status >= 500 && status <= 599;
        };
    }

    private static Completion parseCompletion(String body) {
        try {
            JSONObject result = JSON.parseObject(body);
            if (result == null) {
                throw new IllegalStateException("Empty chat completion response");
            }
            JSONArray choices = result.getJSONArray("choices");
            JSONObject choice = choices == null || choices.isEmpty() ? null : choices.getJSONObject(0);
            JSONObject message = choice == null ? null : choice.getJSONObject("message");
            if (message != null && message.get("content") instanceof String content) {
                return new Completion(content, result.getJSONObject("usage"));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Invalid chat completion response", e);
        }
        throw new IllegalStateException("Chat completion response is missing choices[0].message.content");
    }

    private static URI endpoint(String baseUrl) {
        String url = baseUrl.replaceFirst("/+$", "");
        if (url.endsWith(COMPLETIONS_PATH)) {
            return URI.create(url);
        }
        return URI.create(url + (url.endsWith("/v1") ? "" : "/v1") + COMPLETIONS_PATH);
    }
}
