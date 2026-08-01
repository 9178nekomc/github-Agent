package com.ai.github.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 根据自然语言任务描述，调用AI生成一个完整的、可运行的项目（多文件），并写入本地工作目录。
 */
public class ProjectGenerator {

    private final AIClient aiClient;

    public ProjectGenerator(AIClient aiClient) {
        this.aiClient = aiClient;
    }

    public static class GeneratedProject {
        public String repoName;
        public String description;
        public Path directory;
    }

    public GeneratedProject generate(String taskDescription, Path workspaceDir) throws IOException {
        String system = "你是一名资深软件工程师。根据用户的任务描述，生成一个可以直接编译/运行的完整项目。\n"
                + "严格按以下JSON结构返回，不要有任何多余文字：\n"
                + "{\n"
                + "  \"repoName\": \"kebab-case的仓库名，简短且能反映项目内容，例如 spring-login-demo\",\n"
                + "  \"description\": \"一句话项目描述\",\n"
                + "  \"files\": [\n"
                + "    {\"path\": \"相对路径，如 pom.xml 或 src/main/java/com/example/App.java\", \"content\": \"文件完整内容\"}\n"
                + "  ]\n"
                + "}\n"
                + "要求：\n"
                + "1. files中必须是完整的、可编译运行的代码，不能有省略或TODO占位。\n"
                + "2. 必须包含README.md，说明项目用途、如何构建、如何运行。\n"
                + "3. 如果是Maven Java项目，必须包含完整可用的pom.xml。\n"
                + "4. 代码需要有必要的注释，风格简洁清晰。";

        JsonNode result = aiClient.chatJson(system, "任务：" + taskDescription);

        GeneratedProject project = new GeneratedProject();
        project.repoName = sanitizeRepoName(result.path("repoName").asText(""), taskDescription);
        project.description = result.path("description").asText("AI生成的项目");

        Path dir = workspaceDir.resolve(project.repoName);
        Files.createDirectories(dir);

        JsonNode files = result.path("files");
        if (!files.isArray() || files.isEmpty()) {
            throw new IOException("AI未返回任何文件内容，原始响应: " + result);
        }

        int written = 0;
        for (JsonNode file : files) {
            String relativePath = file.path("path").asText("");
            String content = file.path("content").asText("");
            if (relativePath.isBlank()) {
                continue;
            }

            Path filePath = dir.resolve(relativePath).normalize();
            if (!filePath.startsWith(dir)) {
                // 防止AI返回的路径逃逸出工作目录（路径穿越防护）
                continue;
            }
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            written++;
        }

        if (written == 0) {
            throw new IOException("AI返回的文件列表中没有有效文件");
        }

        project.directory = dir;
        return project;
    }

    private String sanitizeRepoName(String candidate, String fallbackSeed) {
        String base = candidate == null || candidate.isBlank()
                ? "ai-project-" + Math.abs(fallbackSeed.hashCode())
                : candidate;
        String cleaned = base.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (cleaned.isBlank()) {
            cleaned = "ai-project-" + System.currentTimeMillis();
        }
        return cleaned;
    }
}
