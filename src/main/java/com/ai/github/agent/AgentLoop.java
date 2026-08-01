package com.ai.github.agent;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 对话主循环：把用户的自然语言输入交给AI做意图分类，
 * 得到结构化JSON（intent + params）后分发给GitHubActions执行真实操作。
 */
public class AgentLoop {

    private final AIClient aiClient;
    private final GitHubActions actions;

    public AgentLoop(AIClient aiClient, GitHubActions actions) {
        this.aiClient = aiClient;
        this.actions = actions;
    }

    public void handle(String userInput) {
        try {
            JsonNode decision = classifyIntent(userInput);
            String intent = decision.path("intent").asText("help");
            JsonNode params = decision.path("params");

            switch (intent) {
                case "create_project" -> {
                    String task = params.path("task").asText(userInput);
                    String url = actions.createProjectAndPublish(task);
                    System.out.println("✅ 完成！仓库地址: " + url);
                }
                case "star" -> {
                    String query = params.path("query").asText(userInput);
                    System.out.println(actions.starProjectByQuery(query));
                }
                case "follow" -> {
                    String username = params.path("username").asText("");
                    if (username.isBlank()) {
                        System.out.println("⚠️ 请告诉我要关注哪个用户名，例如：关注 torvalds");
                    } else {
                        System.out.println(actions.followUserByName(username));
                    }
                }
                case "browse" -> {
                    String language = params.path("language").asText("");
                    System.out.println(actions.browseAndDiscover(language));
                }
                case "help" -> printHelp();
                default -> System.out.println("🤔 没太理解这个指令，输入 帮助 查看我能做什么。");
            }
        } catch (Exception e) {
            System.out.println("❌ 执行出错: " + e.getMessage());
        }
    }

    private JsonNode classifyIntent(String userInput) throws Exception {
        String system = "你是一个GitHub账号操作助手的意图分类器。"
                + "根据用户输入，判断意图并提取参数，严格返回如下JSON格式之一（只返回一个对象）：\n"
                + "{\"intent\": \"create_project\", \"params\": {\"task\": \"项目需求的完整描述\"}}\n"
                + "{\"intent\": \"star\", \"params\": {\"query\": \"用于搜索仓库的关键词\"}}\n"
                + "{\"intent\": \"follow\", \"params\": {\"username\": \"GitHub用户名\"}}\n"
                + "{\"intent\": \"browse\", \"params\": {\"language\": \"编程语言，找不到则为空字符串\"}}\n"
                + "{\"intent\": \"help\", \"params\": {}}\n"
                + "无法判断意图时，返回 help。";
        return aiClient.chatJson(system, userInput);
    }

    public void printHelp() {
        System.out.println("""
                🤖 我可以帮你做这些事：
                  - 创建项目并发布到GitHub，例如：帮我创建一个Spring Boot登录项目
                  - Star一个项目，例如：帮我star一个Java的爬虫项目
                  - 关注用户，例如：关注 torvalds
                  - 浏览热门项目，例如：看看有什么热门的Python项目
                  - 输入 退出 结束对话
                """);
    }
}
