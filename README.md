# Confluence MCP Server ![Java](https://img.shields.io/badge/Java-17+-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)]() ![CI/CD](https://github.com/your-org/confluence-mcp-server/actions/workflows/ci-cd.yml/badge.svg)

> 基于Spring Boot 3.2构建的企业级Confluence MCP服务器，提供标准的Model Context Protocol接口，支持Confluence内容搜索和页面访问。

## ✨ 核心特性

- **标准化MCP协议**：完全遵循Model Context Protocol规范
- **企业级架构**：模块化设计，清晰的职责分离
- **高性能HTTP客户端**：连接池管理，超时控制
- **完整测试覆盖**：单元测试 + 集成测试 + CI/CD流水线
- **生产就绪**：完善的异常处理、日志监控和安全配置

## 🛠️ 技术栈

| 组件           | 技术选型                     |
|---------------|----------------------------|
| 开发语言       | Java 17+                   |
| 核心框架       | Spring Boot 3.2            |
| MCP协议支持    | Spring AI MCP              |
| 构建工具       | Maven                      |
| 测试框架       | JUnit 5, Mockito           |
| HTTP客户端     | Apache HttpClient 5        |

## 🚀 快速开始

### 环境要求

- JDK 17 或更高版本
- Maven 3.6+
- Confluence服务器访问权限

### 安装部署

```bash
# 1. 克隆项目
git clone https://github.com/your-org/confluence-mcp-server.git
cd confluence-mcp-server

# 2. 构建项目
mvn clean package

# 3. 配置环境变量
export CONFLUENCE_URL=your_confluence_url
export CONFLUENCE_USERNAME=your_username
export CONFLUENCE_PASSWORD=your_password

# 4. 运行应用
java -jar target/confluence-mcp-server-*.jar

# 开发模式
mvn spring-boot:run
```

## ⚙️ 配置说明

创建 `application.yml` 或使用环境变量：

```yaml
server:
  port: 9091

spring:
  application:
    name: confluence-mcp-server
  ai:
    mcp:
      server:
        enabled: true
        name: confluence_mcp_server
        version: 1.0.0
        sse-endpoint: /mcp/sse
        sse-message-endpoint: /api/v1/mcp

confluence:
  url: ${CONFLUENCE_URL:https://your-confluence.com}
  username: ${CONFLUENCE_USERNAME}
  password: ${CONFLUENCE_PASSWORD}
  default-space: ${CONFLUENCE_DEFAULT_SPACE:RP}
  default-content-type: ${CONFLUENCE_DEFAULT_CONTENT_TYPE:page,blogpost}
  default-search-limit: ${CONFLUENCE_DEFAULT_SEARCH_LIMIT:10}
  connection-timeout: ${CONFLUENCE_CONNECTION_TIMEOUT:30000}
  read-timeout: ${CONFLUENCE_READ_TIMEOUT:30000}
```

## 📡 MCP工具接口

### searchConfluence
在Confluence中搜索内容

**参数：**
- `searchKeyword`: 搜索关键词（必需）
- `space`: 空间标识
- `contentType`: 内容类型（page/blogpost）
- `limit`: 结果数量限制

### getConfluencePage
获取指定页面内容

**参数：**
- `pageId`: Confluence页面ID（必需）

## 🏗️ 项目结构

```
src/
├── main/
│   ├── java/com/confluence/mcp/
│   │   ├── config/           # 配置类
│   │   │   ├── ConfluenceConfig.java
│   │   │   ├── HttpClientConfig.java
│   │   │   └── McpConfig.java
│   │   ├── exception/        # 异常处理
│   │   │   ├── ConfluenceException.java
│   │   │   └── ExceptionHandlerUtil.java
│   │   ├── tool/            # MCP工具实现
│   │   │   └── ConfluenceTool.java
│   │   ├── util/            # 工具类
│   │   │   ├── HttpClientUtil.java
│   │   │   └── JsonParserUtil.java
│   │   └── ConfluenceMcpServerApplication.java
│   └── resources/
│       └── application.yml
└── test/
    └── java/com/confluence/mcp/
        └── tool/            # 测试类
            ├── ConfluenceToolMockTest.java
            └── ConfluenceToolTest.java
```

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=ConfluenceToolTest

# 生成测试报告
mvn jacoco:report
```

## 🔧 开发规范

- **代码风格**：遵循Google Java风格指南
- **异常处理**：统一使用ConfluenceException业务异常
- **日志规范**：SLF4J + 统一格式
- **配置管理**：环境变量注入敏感信息

## 📦 CI/CD流程

GitHub Actions自动化流水线包含：
1. 代码编译和依赖检查
2. 单元测试和集成测试
3. 代码质量扫描
4. 安全漏洞检测

## 🚨 安全注意事项

- 🔐 使用环境变量管理Confluence凭证
- 🛡️ 所有输入参数进行验证和过滤
- ⏱️ 配置合理的网络超时时间
- 📊 启用详细的访问日志和监控

## 📄 License

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🤝 贡献

欢迎提交Issue和Pull Request！贡献前请确保：
- 代码符合项目代码风格
- 包含相应的单元测试
- 更新相关文档