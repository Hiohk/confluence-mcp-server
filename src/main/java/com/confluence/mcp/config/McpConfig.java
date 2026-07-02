package com.confluence.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.confluence.mcp.tool.ConfluenceTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ToolCallbackProvider confluenceToolProvider(ConfluenceTool confluenceTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(confluenceTool)
                .build();
    }
}
