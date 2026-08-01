package com.ai.github.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * 基于GitHub官方REST API（https://api.github.com）的客户端封装。
 * 所有操作都通过Personal Access Token鉴权，走官方支持的接口，
 * 不做任何页面模拟/DOM解析，天然遵守GitHub的接口速率限制和使用条款。
 */
public class GitHubApiClient {

    private static final String API_BASE = "https://api.github.com";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String token;
    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHubApiClient(String token) {
        this.token = token;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    private Request.Builder baseRequest(String path) {
        return new Request.Builder()
                .url(API_BASE + path)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28");
    }

    public JsonNode getAuthenticatedUser() throws IOException {
        Request request = baseRequest("/user").get().build();
        return execute(request);
    }

    public JsonNode createRepository(String name, String description, boolean isPrivate) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("name", name);
        body.put("description", description == null ? "" : description);
        body.put("private", isPrivate);
        body.put("auto_init", false);

        Request request = baseRequest("/user/repos")
                .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
                .build();
        return execute(request);
    }

    public boolean deleteRepository(String owner, String repo) throws IOException {
        Request request = baseRequest("/repos/" + owner + "/" + repo).delete().build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public JsonNode getRepository(String owner, String repo) throws IOException {
        Request request = baseRequest("/repos/" + owner + "/" + repo).get().build();
        return execute(request);
    }

    public JsonNode createOrUpdateFile(String owner, String repo, String path, String message,
                                        String content, String branch, String existingSha) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("message", message);
        body.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
        if (branch != null && !branch.isBlank()) {
            body.put("branch", branch);
        }
        if (existingSha != null && !existingSha.isBlank()) {
            body.put("sha", existingSha);
        }

        Request request = baseRequest("/repos/" + owner + "/" + repo + "/contents/" + path)
                .put(RequestBody.create(mapper.writeValueAsString(body), JSON))
                .build();
        return execute(request);
    }

    public boolean starRepository(String owner, String repo) throws IOException {
        Request request = baseRequest("/user/starred/" + owner + "/" + repo)
                .put(RequestBody.create(new byte[0], null))
                .build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public boolean unstarRepository(String owner, String repo) throws IOException {
        Request request = baseRequest("/user/starred/" + owner + "/" + repo).delete().build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public boolean isStarring(String owner, String repo) throws IOException {
        Request request = baseRequest("/user/starred/" + owner + "/" + repo).get().build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public boolean followUser(String username) throws IOException {
        Request request = baseRequest("/user/following/" + username)
                .put(RequestBody.create(new byte[0], null))
                .build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public boolean unfollowUser(String username) throws IOException {
        Request request = baseRequest("/user/following/" + username).delete().build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public boolean isFollowing(String username) throws IOException {
        Request request = baseRequest("/user/following/" + username).get().build();
        try (Response response = http.newCall(request).execute()) {
            return response.code() == 204;
        }
    }

    public JsonNode searchRepositories(String query, String sort, String order, int perPage) throws IOException {
        HttpUrl url = HttpUrl.parse(API_BASE + "/search/repositories").newBuilder()
                .addQueryParameter("q", query)
                .addQueryParameter("sort", sort)
                .addQueryParameter("order", order)
                .addQueryParameter("per_page", String.valueOf(perPage))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .get()
                .build();
        return execute(request);
    }

    public String getReadmeText(String owner, String repo) throws IOException {
        Request request = baseRequest("/repos/" + owner + "/" + repo + "/readme").get().build();
        try (Response response = http.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                return "";
            }
            JsonNode json = mapper.readTree(raw);
            String contentB64 = json.path("content").asText("");
            String cleaned = contentB64.replace("\n", "");
            if (cleaned.isBlank()) {
                return "";
            }
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            return new String(decoded, StandardCharsets.UTF_8);
        }
    }

    /**
     * 供Git over HTTPS推送使用的凭据（与API鉴权共用同一个Token）。
     */
    public String getTokenForGitOverHttp() {
        return token;
    }

    private JsonNode execute(Request request) throws IOException {
        try (Response response = http.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("GitHub API 调用失败: HTTP " + response.code() + " - " + raw);
            }
            return mapper.readTree(raw);
        }
    }
}
