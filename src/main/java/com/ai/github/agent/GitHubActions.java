package com.ai.github.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 把底层的 GitHubApiClient / GitOperations / ProjectGenerator 组合成
 * "创建项目并发布"、"star"、"follow"、"浏览发现" 这些完整业务流程。
 */
public class GitHubActions {

    private final GitHubApiClient github;
    private final GitOperations gitOps;
    private final ProjectGenerator projectGenerator;
    private final String githubUsername;
    private final Path workspaceDir;

    public GitHubActions(GitHubApiClient github, GitOperations gitOps, ProjectGenerator projectGenerator,
                          String githubUsername, String workspaceDir) {
        this.github = github;
        this.gitOps = gitOps;
        this.projectGenerator = projectGenerator;
        this.githubUsername = githubUsername;
        this.workspaceDir = Paths.get(workspaceDir);
    }

    public String createProjectAndPublish(String taskDescription) throws Exception {
        System.out.println("📝 正在让AI生成项目...");
        ProjectGenerator.GeneratedProject project = projectGenerator.generate(taskDescription, workspaceDir);
        System.out.println("🧠 AI生成完成，仓库名: " + project.repoName);

        System.out.println("📦 正在GitHub上创建仓库...");
        JsonNode repoResult = github.createRepository(project.repoName, project.description, false);
        String cloneUrl = repoResult.path("clone_url").asText();
        String htmlUrl = repoResult.path("html_url").asText();
        System.out.println("✅ 仓库创建成功: " + htmlUrl);

        System.out.println("💾 正在初始化本地Git仓库...");
        File dir = project.directory.toFile();
        Git git = gitOps.initRepo(dir);
        try {
            gitOps.ensureMainBranch(git);
            gitOps.addAll(git);
            gitOps.commit(git, "AI generated project: " + project.description,
                    githubUsername, githubUsername + "@users.noreply.github.com");
            gitOps.addRemote(git, "origin", cloneUrl);

            System.out.println("📤 正在推送到GitHub...");
            gitOps.push(git, githubUsername, github.getTokenForGitOverHttp());
        } finally {
            git.close();
        }

        System.out.println("🔗 仓库地址: " + htmlUrl);
        return htmlUrl;
    }

    public String starProjectByQuery(String query) throws IOException {
        System.out.println("🔍 正在搜索仓库: " + query);
        JsonNode result = github.searchRepositories(query, "stars", "desc", 5);
        JsonNode items = result.path("items");
        if (!items.isArray() || items.isEmpty()) {
            return "未找到匹配 \"" + query + "\" 的仓库。";
        }

        JsonNode top = items.get(0);
        String fullName = top.path("full_name").asText();
        String[] parts = fullName.split("/", 2);
        github.starRepository(parts[0], parts[1]);

        String url = top.path("html_url").asText();
        System.out.println("⭐ 已star: " + fullName);
        return "已star项目: " + fullName + " (" + url + ")";
    }

    public String followUserByName(String username) throws IOException {
        github.followUser(username);
        System.out.println("👤 已关注: " + username);
        return "已关注用户: " + username;
    }

    public String browseAndDiscover(String language) throws IOException {
        String query = "stars:>500" + (language == null || language.isBlank() ? "" : " language:" + language);
        JsonNode result = github.searchRepositories(query, "stars", "desc", 10);
        JsonNode items = result.path("items");

        List<String> lines = new ArrayList<>();
        for (JsonNode item : items) {
            lines.add(String.format("  - %s (⭐%d) - %s",
                    item.path("full_name").asText(),
                    item.path("stargazers_count").asInt(),
                    item.path("description").asText("")));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 找到 ").append(lines.size()).append(" 个热门项目:\n");
        sb.append(String.join("\n", lines));
        return sb.toString();
    }
}
