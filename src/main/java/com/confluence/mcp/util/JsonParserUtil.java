package com.confluence.mcp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    private final ObjectMapper objectMapper;

    /**
     * 解析Confluence搜索结果的JSON响应
     *
     * @param jsonResponse Confluence搜索API返回的JSON字符串
     * @return 页面列表
     * @throws ConfluenceJsonParseException 当JSON解析失败
     */
    public List<ConfluencePage> parseSearchResults(String jsonResponse) {
        log.debug("开始解析Confluence搜索结果JSON，响应长度: {}", jsonResponse.length());
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");
            int resultCount = resultsNode.isArray() ? resultsNode.size() : 0;
            log.debug("解析到{}个结果", resultCount);

            List<ConfluencePage> pages = new ArrayList<>();
            if (resultsNode.isArray()) {
                for (JsonNode resultNode : resultsNode) {
                    ConfluencePage page = new ConfluencePage();
                    page.setId(nullSafeText(resultNode, "id"));
                    page.setTitle(nullSafeText(resultNode, "title"));
                    page.setWebUrl(nullSafeText(resultNode.path("_links"), "webui"));
                    page.setSpaceKey(nullSafeText(resultNode.path("space"), "key"));
                    page.setExcerpt(nullSafeText(resultNode, "excerpt"));
                    pages.add(page);
                }
            }

            log.info("解析Confluence搜索结果完成，找到{}个页面", pages.size());
            return pages;
        } catch (JsonProcessingException e) {
            log.error("解析搜索结果JSON失败: {}", e.getMessage());
            throw new ConfluenceJsonParseException("解析搜索结果失败，JSON格式异常", e);
        }
    }

    /**
     * 解析页面内容的JSON响应
     *
     * @param jsonResponse Confluence REST API返回的JSON字符串
     * @return 页面内容，如果无法提取则返回空Optional
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
        } catch (JsonProcessingException e) {
            log.warn("解析页面内容失败: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("解析页面内容时发生未知异常: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 安全地获取JSON节点的文本值，避免NPE
     */
    private String nullSafeText(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? "" : field.asText();
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
        private String excerpt;
    }

    /**
     * JSON解析专用异常
     */
    public static class ConfluenceJsonParseException extends RuntimeException {
        public ConfluenceJsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
