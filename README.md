# Confluence MCP Server ![Java](https://img.shields.io/badge/Java-17+-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green) [![License](https://img.shields.io/badge/license-MIT-blue)]()

> 基于 Spring Boot 的 Confluence MCP (Model Context Protocol) 服务器，提供 Confluence 内容的搜索和获取功能

## 项目概述

这是一个 MCP (Model Context Protocol) 服务器，专门用于与 Atlassian Confluence 集成，提供以下功能：

✅ **核心功能**
- 搜索 Confluence 页面内容
- 获取指定 Confluence 页面的详细内容
- 支持按空间和内容类型进行筛选

🔧 **扩展能力**
- 易于添加新的 MCP 工具
- 支持 SSE (Server-Sent Events) 协议

## 🛠️ 技术栈

| 类别     | 技术                           |
| -------- | ------------------------------ |
| 语言     | Java 17+                       |
| 框架     | Spring Boot 3.x, Spring AI MCP |
| 构建工具 | Maven                          |
| 协议     | MCP (Model Context Protocol)   |

## 🚀 快速开始

### 前置要求

- ☑️ Java 17 或更高版本 ([下载JDK](https://adoptium.net/))
- ☑️ Maven 3.6+ ([安装指南](https://maven.apache.org/install.html))
- ☑️ Confluence 服务器访问权限

### 安装和运行

```bash
# 1. 克隆项目
git clone https://github.com/your-org/confluence-mcp-server.git
cd confluence-mcp-server

# 2. 配置应用 (复制开发配置模板)
cp src/main/resources/application.yml src/main/resources/application-dev.yml

# 3. 构建项目
mvn clean package

# 4. 运行应用 (开发模式)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者直接运行 jar 文件 (生产模式)
java -jar target/confluence-mcp-server-*.jar
```

> 💡 提示: 开发时使用 `application-dev.yml` 配置，生产环境建议使用环境变量

## ⚙️ 配置说明

### 主要配置项

```yaml
server:
  port: 9091  # 服务端口

spring:
  application:
    name: confluence_mcp_server  # 应用名称

confluence:
  url: https://your-confluence-server.com  # Confluence 地址
  username: your-username                 # 用户名
  password: your-password                 # 密码
```

### MCP 配置

| 端点          | 描述                            |
| ------------- | ------------------------------- |
| `/mcp/sse`    | SSE (Server-Sent Events) 主端点 |
| `/api/v1/mcp` | SSE 消息端点                    |

## 📡 API 功能

### 可用工具

| 工具                | 描述                         | 参数                                                                                                 |
| ------------------- | ---------------------------- | ---------------------------------------------------------------------------------------------------- |
| `searchConfluence`  | 在 Confluence 中搜索内容     | `searchKeyword`: 搜索关键词<br>`space`: 空间标识<br>`contentType`: 内容类型<br>`limit`: 结果数量限制 |
| `getConfluencePage` | 获取指定 Confluence 页面内容 | `pageId`: 页面 ID                                                                                    |

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

## ⚠️ 注意事项

- 🛡️ **安全建议**:
  - 生产环境使用环境变量或密钥管理工具存储凭证
  - 避免在代码中硬编码敏感信息
- 🌐 **连接要求**:
  - 确保 Confluence 服务器可访问
  - 确保凭证有足够权限
- 🔄 **版本兼容**:
  - 与 Confluence 7.x+ 版本兼容
  - 如需支持更早版本，请联系开发团队

## ❓ 支持

遇到问题? 请:

1. 检查 [常见问题]()
2. [提交 Issue]()
3. 联系开发团队: dev-team@example.com

## 📜 License

MIT License - 详见 [LICENSE](LICENSE) 文件