package com.ai.github.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== GitHub AI Agent ===");
        System.out.println("(基于官方GitHub REST API，操作你已授权的GitHub账号)\n");

        try (Scanner scanner = new Scanner(System.in)) {
            ConfigManager config = ConfigManager.loadOrSetup(scanner);

            AIClient aiClient = new AIClient(config.getOpenaiEndpoint(), config.getOpenaiKey(), config.getOpenaiModel());
            GitHubApiClient githubClient = new GitHubApiClient(config.getGithubToken());

            System.out.println("🔐 正在验证GitHub Token...");
            JsonNode me;
            try {
                me = githubClient.getAuthenticatedUser();
            } catch (Exception e) {
                System.out.println("❌ Token验证失败: " + e.getMessage());
                System.out.println("请检查 config.properties 中的 github.token 是否正确，程序退出。");
                return;
            }

            String login = me.path("login").asText("");
            if (login.isBlank()) {
                System.out.println("❌ Token验证失败，未能获取到用户名，程序退出。");
                return;
            }
            System.out.println("✅ 已登录GitHub账号: " + login + "\n");

            GitOperations gitOps = new GitOperations();
            ProjectGenerator projectGenerator = new ProjectGenerator(aiClient);
            GitHubActions actions = new GitHubActions(githubClient, gitOps, projectGenerator,
                    login, config.getWorkspaceDir());
            AgentLoop agentLoop = new AgentLoop(aiClient, actions);

            agentLoop.printHelp();

            while (true) {
                System.out.print("💬 你说: ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equals("退出") || input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("👋 再见！");
                    break;
                }
                agentLoop.handle(input);
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("❌ 程序出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
