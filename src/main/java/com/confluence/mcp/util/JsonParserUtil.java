package com.confluence.mcp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON解析工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonParserUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析Confluence搜索结果的JSON响应
     */
    public List<ConfluencePage> parseSearchResults(String jsonResponse) throws Exception {
        log.debug("开始解析Confluence搜索结果JSON，响应长度: {}", jsonResponse.length());
        JsonNode rootNode = objectMapper.readTree(jsonResponse);
        JsonNode resultsNode = rootNode.path("results");
        log.debug("解析到{}个结果", resultsNode.isArray() ? resultsNode.size() : 0);

        List<ConfluencePage> pages = new ArrayList<>();

        if (resultsNode.isArray()) {
            for (JsonNode resultNode : resultsNode) {
                ConfluencePage page = new ConfluencePage();
                page.setId(resultNode.path("id").asText());
                page.setTitle(resultNode.path("title").asText());
                page.setWebUrl(resultNode.path("_links").path("webui").asText());
                page.setSpaceKey(resultNode.path("space").path("key").asText());
                pages.add(page);
            }
        }

        log.info("解析Confluence搜索结果完成，找到{}个页面", pages.size());
        return pages;
    }

    /**
     * 解析页面内容的JSON响应
     */
    public Optional<String> parsePageContent(String jsonResponse) {
        log.debug("开始解析页面内容JSON，响应长度: {}", jsonResponse.length());
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode bodyNode = rootNode.path("body");
            JsonNode viewNode = bodyNode.path("view");
            JsonNode valueNode = viewNode.path("value");

            if (valueNode.isTextual()) {
                String content = valueNode.asText();
                log.debug("解析页面内容成功，内容长度: {}字符", content.length());
                return Optional.of(content);
            }
            log.debug("页面内容字段不是文本类型或无内容");
            return Optional.empty();
        } catch (Exception e) {
            log.warn("解析页面内容失败: {}", e.getMessage());
            log.debug("解析失败的JSON内容: {}",
                     jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
        }
        return Optional.empty();
    }

    /**
     * Confluence页面数据类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfluencePage {
        private String id;
        private String title;
        private String webUrl;
        private String spaceKey;
    }
}