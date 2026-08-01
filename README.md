# GitHub AI Agent

一个命令行AI助手，基于**官方GitHub REST API**操作你自己已授权的GitHub账号：创建仓库、AI生成代码并提交、star项目、关注用户、浏览发现热门项目。

与"浏览器模拟人类操作网页"的方案不同，本项目直接调用GitHub官方接口，天然遵守其接口速率限制，不需要处理验证码/图形验证，也不需要维护DOM选择器。

## 功能

- 💬 自然语言对话式操作（AI自动判断你想做什么）
- 📦 AI根据需求描述生成完整项目代码，自动创建仓库并推送（Git操作基于JGit，无需本机安装git）
- ⭐ 按关键词搜索并star仓库
- 👤 关注指定用户
- 🔭 按语言浏览高star热门项目

## 环境要求

- Java 21+
- Maven 3.8+

## 快速开始

### 1. 准备GitHub Personal Access Token

1. 打开 https://github.com/settings/tokens
2. 生成新Token（classic）
3. 勾选权限：`repo`（创建/推送仓库）、`user`（关注/查看用户信息）；如果需要删除仓库功能，额外勾选 `delete_repo`

### 2. 准备AI API密钥

任意兼容OpenAI `/chat/completions` 协议的服务商都可以，默认使用 `https://api.openai.com/v1`。

### 3. 构建

```bash
mvn clean package
```

会在 `target/github-agent.jar` 生成可直接运行的fat jar。

### 4. 运行

```bash
java -jar target/github-agent.jar
```

首次运行会交互式引导你输入：AI API端点、密钥、模型名，以及GitHub Token和用户名，配置会保存到 `config.properties`，之后自动复用，无需重复输入。

### 5. 开始对话

```
💬 你说: 帮我创建一个Spring Boot登录demo项目
💬 你说: 帮我star一个Java写的爬虫框架
💬 你说: 关注 torvalds
💬 你说: 看看有什么热门的Python项目
💬 你说: 退出
```

## 项目结构

```
com.ai.github.agent/
├── Main.java              # 命令行入口
├── ConfigManager.java     # 配置管理（API端点/Key/模型/GitHub Token）
├── AIClient.java          # AI API调用（OpenAI兼容协议）
├── GitHubApiClient.java   # GitHub官方REST API封装
├── GitOperations.java     # 本地Git操作（基于JGit）
├── ProjectGenerator.java  # AI生成项目代码并写入磁盘
├── GitHubActions.java     # 业务流程编排（创建发布/star/follow/浏览）
├── AgentLoop.java         # 对话主循环（意图分类→分发执行）
└── JsonUtils.java         # JSON文本清洗工具
```

## 配置文件示例（config.properties）

```properties
openai.endpoint=https://api.openai.com/v1
openai.key=sk-xxx
openai.model=gpt-4-turbo-preview
github.token=ghp_xxx
github.username=your-username
workspace.dir=workspace
```

## 说明

- 所有GitHub操作均使用你自己的Token，以你的身份执行，可在 https://github.com/settings/tokens 随时查看/吊销。
- `workspace/` 目录用于存放AI生成的项目源码，`.git`仓库也建立在这里，push成功后可自行保留或清理。
