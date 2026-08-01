package com.ai.github.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 通用AI客户端，兼容OpenAI风格的 /chat/completions 接口。
 * 支持自定义endpoint，因此也能接入其他兼容OpenAI协议的服务商。
 */
public class AIClient {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public AIClient(String endpoint, String apiKey, String model) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) throws IOException {
        return chat(systemPrompt, userPrompt, 0.4);
    }

    public String chat(String systemPrompt, String userPrompt, double temperature) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", temperature);

        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sys = mapper.createObjectNode();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            messages.add(sys);
        }
        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        RequestBody body = RequestBody.create(mapper.writeValueAsString(root), JSON);
        Request request = new Request.Builder()
                .url(endpoint + "/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = http.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("AI API 调用失败: HTTP " + response.code() + " - " + raw);
            }
            JsonNode json = mapper.readTree(raw);
            JsonNode choices = json.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new IOException("AI API 返回中没有choices字段: " + raw);
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new IOException("AI API 返回格式异常: " + raw);
            }
            return message.get("content").asText();
        }
    }

    /**
     * 强制要求AI只返回JSON，并自动剥离可能存在的Markdown代码块围栏后解析。
     */
    public JsonNode chatJson(String systemPrompt, String userPrompt) throws IOException {
        String enforcedSystem = (systemPrompt == null ? "" : systemPrompt + "\n\n")
                + "严格要求：只返回合法JSON文本本身，不要包含任何解释性文字，不要使用Markdown代码块围栏（```），不要有多余的前后缀说明。";

        String raw = chat(enforcedSystem, userPrompt, 0.2);
        String cleaned = JsonUtils.stripCodeFences(raw);

        try {
            return mapper.readTree(cleaned);
        } catch (Exception e) {
            throw new IOException("AI返回内容不是合法JSON，原始内容: " + raw, e);
        }
    }
}
