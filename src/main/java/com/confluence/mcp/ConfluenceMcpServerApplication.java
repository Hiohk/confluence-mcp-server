package com.confluence.mcp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Confluence MCP Server应用程序入口
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class ConfluenceMcpServerApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(ConfluenceMcpServerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("========================================");
        log.info("  Confluence MCP Server 启动完成");
        log.info("  服务端口: 9090");
        log.info("  MCP 端点: /mcp/sse");
        log.info("  健康检查: /actuator/health");
        log.info("========================================");
    }
}
