# Confluence MCP Server

基于 Spring Boot 的 Confluence MCP (Model Context Protocol) 服务器，提供 Confluence 内容的搜索和获取功能。

## 项目概述

这是一个 MCP (Model Context Protocol) 服务器，专门用于与 Atlassian Confluence 集成，提供以下功能：
- 搜索 Confluence 页面内容
- 获取指定 Confluence 页面的详细内容
- 支持按空间和内容类型进行筛选

## 技术栈

- **Java 17+**
- **Spring Boot 3.x**
- **Spring AI MCP**
- **Maven**

## 快速开始

### 前置要求

1. Java 17 或更高版本
2. Maven 3.6+
3. Confluence 服务器访问权限

### 安装和运行

1. 克隆项目
```bash
git clone <项目地址>
cd confluence-mcp-server
```

2. 配置应用
复制 `src/main/resources/application-dev.yml` 并根据实际情况修改配置：

```yaml
confluence:
  url: https://your-confluence-server.com
  username: your-username
  password: your-password
```

3. 构建项目
```bash
mvn clean package
```

4. 运行应用
```bash
mvn spring-boot:run
```

或者直接运行 jar 文件：
```bash
java -jar target/confluence-mcp-server-1.0.0.jar
```

## 配置说明

### 主要配置项

在配置文件中配置：

```yaml
server:
  port: 9091

spring:
  application:
    name: confluence_mcp_server

confluence:
  url: https://your-confluence-server.com
  username: your-username
  password: your-password
```

### MCP 配置

应用默认启用 MCP 服务器，提供以下端点：
- SSE 端点: `/mcp/sse`
- SSE 消息端点: `/api/v1/mcp`

## API 功能

### 可用工具

1. **searchConfluence**
   - 描述: 在 Confluence 中搜索内容
   - 参数:
     - searchKeyword: 搜索关键词
     - space: 空间标识
     - contentType: 内容类型
     - limit: 结果数量限制

2. **getConfluencePage**
   - 描述: 获取指定 Confluence 页面内容
   - 参数:
     - pageId: 页面 ID

## 开发指南

### 项目结构

```
src/
├── main/
│   ├── java/com/confluence/mcp/
│   │   ├── config/          # 配置类
│   │   ├── tool/            # MCP 工具实现
│   │   └── ConfluenceMcpServerApplication.java
│   └── resources/
│       ├── application.yml          # 生产配置
│       └── application-dev.yml      # 开发配置（已加入.gitignore）
└── test/                     # 测试代码
```

### 添加新的 MCP 工具

1. 在 `tool/` 包下创建新的工具类
2. 实现相应的工具方法
3. 在配置类中注册工具

## 注意事项

1. **安全警告**: `application-dev.yml` 文件包含敏感信息（用户名、密码），已被添加到 `.gitignore` 中避免提交到版本库。
2. 生产环境请使用环境变量或安全的配置管理方式存储凭证。
3. 确保 Confluence 服务器可访问，并且提供的凭证有足够的权限。

## 支持

如有问题，请提交 Issue 或联系开发团队。