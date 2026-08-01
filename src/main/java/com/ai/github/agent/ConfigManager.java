package com.ai.github.agent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

/**
 * 负责加载/保存 config.properties。
 * 首次运行时交互式收集：AI API端点、密钥、模型名，以及GitHub Personal Access Token和用户名。
 * Token保存后即可长期复用，等价于"登录状态持久化"，无需每次手动登录。
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";

    private String openaiEndpoint;
    private String openaiKey;
    private String openaiModel;
    private String githubToken;
    private String githubUsername;
    private String workspaceDir;

    public static ConfigManager loadOrSetup(Scanner scanner) throws IOException {
        ConfigManager cfg = new ConfigManager();
        Path path = Paths.get(CONFIG_FILE);

        if (Files.exists(path)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            }
            cfg.openaiEndpoint = p.getProperty("openai.endpoint", "https://api.openai.com/v1");
            cfg.openaiKey = p.getProperty("openai.key", "");
            cfg.openaiModel = p.getProperty("openai.model", "gpt-4-turbo-preview");
            cfg.githubToken = p.getProperty("github.token", "");
            cfg.githubUsername = p.getProperty("github.username", "");
            cfg.workspaceDir = p.getProperty("workspace.dir", "workspace");

            System.out.println("已加载配置文件 " + CONFIG_FILE + "（如需重新配置，删除该文件后重启程序）。");
            return cfg;
        }

        System.out.println("=== 首次运行，开始配置 ===\n");

        System.out.print("AI API端点 (默认: https://api.openai.com/v1): ");
        String endpoint = scanner.nextLine().trim();
        cfg.openaiEndpoint = endpoint.isEmpty() ? "https://api.openai.com/v1" : endpoint;

        System.out.print("AI API密钥: ");
        cfg.openaiKey = scanner.nextLine().trim();

        System.out.print("模型名称 (默认: gpt-4-turbo-preview): ");
        String model = scanner.nextLine().trim();
        cfg.openaiModel = model.isEmpty() ? "gpt-4-turbo-preview" : model;

        System.out.println();
        System.out.println("接下来配置GitHub账号（使用官方Personal Access Token，替代手动网页登录）：");
        System.out.println("  1. 打开 https://github.com/settings/tokens");
        System.out.println("  2. 生成新Token（classic），勾选权限: repo, user, delete_repo（如需删除仓库功能）");
        System.out.print("GitHub Personal Access Token: ");
        cfg.githubToken = scanner.nextLine().trim();

        System.out.print("GitHub用户名: ");
        cfg.githubUsername = scanner.nextLine().trim();

        cfg.workspaceDir = "workspace";

        cfg.save();
        System.out.println("\n配置已保存到 " + CONFIG_FILE + "，下次启动将自动复用，无需重新输入。");
        return cfg;
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("openai.endpoint", nullToEmpty(openaiEndpoint));
        p.setProperty("openai.key", nullToEmpty(openaiKey));
        p.setProperty("openai.model", nullToEmpty(openaiModel));
        p.setProperty("github.token", nullToEmpty(githubToken));
        p.setProperty("github.username", nullToEmpty(githubUsername));
        p.setProperty("workspace.dir", nullToEmpty(workspaceDir));

        try (OutputStream out = Files.newOutputStream(Paths.get(CONFIG_FILE))) {
            p.store(out, "GitHub AI Agent Configuration");
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public String getOpenaiEndpoint() {
        return openaiEndpoint;
    }

    public String getOpenaiKey() {
        return openaiKey;
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public String getWorkspaceDir() {
        return workspaceDir;
    }
}
