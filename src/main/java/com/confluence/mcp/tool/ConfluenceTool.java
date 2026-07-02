package com.confluence.mcp.tool;

import com.confluence.mcp.config.ConfluenceConfig;
import com.confluence.mcp.exception.ConfluenceException;
import com.confluence.mcp.util.HttpClientUtil;
import com.confluence.mcp.util.JsonParserUtil;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Confluence MCP工具实现类
 * 提供搜索页面、获取页面内容、文本处理等工具方法
 *
 * <p>工具说明:
 * <ul>
 *   <li>searchConfluence - 只读操作，在Confluence中搜索内容</li>
 *   <li>getConfluencePage - 只读操作，根据页面ID获取页面完整内容</li>
 *   <li>processText - 只读操作，文本处理工具</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfluenceTool {

    private final ConfluenceConfig config;
    private final HttpClientUtil httpClientUtil;
    private final JsonParserUtil jsonParserUtil;
    private final Validator validator;

    private static final int DEFAULT_SUMMARY_LENGTH = 200;
    private static final int FULL_CONTENT_MAX_LENGTH = 5000;

    /**
     * 在Confluence中搜索内容，支持按关键字、空间和类型进行搜索，并返回详细的页面内容摘要。
     * 当space未指定时，默认使用ZXJXTECH空间。
     *
     * <p>此工具为只读操作，不会修改任何数据。
     *
     * @param searchKeyword 搜索关键字，必填
     * @param space 搜索空间，不指定时默认使用ZXJXTECH空间
     * @param contentType 内容类型，默认为page,blogpost
     * @param limit 返回结果数量限制，默认10，最大100
     * @return 搜索结果列表，包含页面ID、标题、链接、内容摘要
     */
    @Tool(name = "searchConfluence", description = "在Confluence中搜索内容，支持按关键字、空间和类型进行搜索，并返回详细的页面内容。当space未指定时，默认使用ZXJXTECH空间。返回搜索结果列表，包含页面ID、标题、访问链接和内容摘要。此工具为只读操作，不会修改任何Confluence数据。")
    public String searchConfluence(
            @NotBlank(message = "搜索关键字不能为空") String searchKeyword,
            String space,
            String contentType,
            @PositiveOrZero(message = "限制数不能为负数") Integer limit) {
        log.info("Confluence搜索请求: 关键字={}, 空间={}, 内容类型={}, 限制={}",
                searchKeyword, space, contentType, limit);

        // 校验搜索关键字
        if (!StringUtils.hasText(searchKeyword)) {
            throw new ConfluenceException("搜索关键字不能为空");
        }

        // 应用默认值
        String targetSpace = StringUtils.hasText(space) ? space : config.getDefaultSpace();
        String targetContentType = StringUtils.hasText(contentType) ? contentType : config.getDefaultContentType();
        int rawLimit = (limit == null) ? config.getDefaultSearchLimit() : limit;
        int targetLimit = Math.min(rawLimit, config.getMaxSearchLimit());

        try {
            String cql = buildCqlQuery(searchKeyword, targetSpace, targetContentType);
            String url = buildSearchUrl(cql, targetLimit);

            String responseString = httpClientUtil.executeGetRequest(url);
            return parseSearchResultsWithContent(responseString, targetLimit);
        } catch (ConfluenceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Confluence搜索失败: 关键字={}, 空间={}, 类型={}, 限制={}",
                    searchKeyword, targetSpace, targetContentType, targetLimit, e);
            throw new ConfluenceException("Confluence搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取Confluence页面内容。根据页面ID获取页面完整内容。
     *
     * <p>此工具为只读操作，不会修改任何数据。
     *
     * @param pageId 页面ID，必填
     * @return 页面内容文本
     */
    @Tool(name = "getConfluencePage", description = "获取Confluence页面内容。根据页面ID获取页面完整内容。此工具为只读操作，不会修改任何Confluence数据。")
    public String getConfluencePage(
            @NotBlank(message = "页面ID不能为空") String pageId) {
        log.info("获取Confluence页面请求: pageId={}", pageId);

        if (!StringUtils.hasText(pageId)) {
            throw new ConfluenceException("页面ID不能为空");
        }

        try {
            // 优先使用REST API获取结构化数据
            String restUrl = buildPageRestApiUrl(pageId);
            String responseString = httpClientUtil.executeGetRequest(restUrl);

            // 尝试从JSON解析内容
            return jsonParserUtil.parsePageContent(responseString)
                    .map(htmlContent -> {
                        if (htmlContent.contains("<html") || htmlContent.contains("<!DOCTYPE")) {
                            return extractMainContentFromHtml(Jsoup.parse(htmlContent));
                        }
                        return htmlContent;
                    })
                    .orElseGet(() -> extractMainContentFromHtml(Jsoup.parse(responseString)));
        } catch (ConfluenceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取Confluence页面失败: pageId={}", pageId, e);
            throw new ConfluenceException("获取Confluence页面失败: " + e.getMessage(), e);
        }
    }

    /**
     * 文本处理工具：计算文本长度、提取摘要、统计字符等，不依赖外部数据。
     *
     * <p>此工具为只读操作，不依赖外部数据。
     *
     * @param text 待处理文本，必填
     * @param maxLength 摘要最大长度，可选，默认200
     * @return 文本处理结果，包含字符统计、摘要和字符频率
     */
    @Tool(name = "processText", description = "文本处理工具：计算文本长度、提取摘要、统计字符频率等。此工具为只读操作，不依赖外部数据。返回文本统计信息、摘要和最常见的5个字符。")
    public String processText(String text, @PositiveOrZero(message = "最大长度不能为负数") Integer maxLength) {
        log.info("文本处理请求: 文本长度={}, 最大长度={}",
                text != null ? text.length() : 0, maxLength);

        if (text == null || text.trim().isEmpty()) {
            throw new ConfluenceException("输入文本为空，无法处理");
        }

        try {
            int summaryLength = (maxLength != null && maxLength > 0) ? maxLength : DEFAULT_SUMMARY_LENGTH;

            int totalLength = text.length();
            int nonSpaceLength = text.replaceAll("\\s+", "").length();
            int wordCount = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            int lineCount = text.split("\n").length;

            String summary;
            if (text.length() <= summaryLength) {
                summary = text;
            } else {
                summary = text.substring(0, summaryLength) + "...";
            }

            java.util.Map<Character, Integer> charFrequency = new java.util.HashMap<>();
            for (char c : text.toCharArray()) {
                if (!Character.isWhitespace(c)) {
                    charFrequency.merge(c, 1, Integer::sum);
                }
            }

            java.util.List<java.util.Map.Entry<Character, Integer>> sortedEntries =
                    new java.util.ArrayList<>(charFrequency.entrySet());
            sortedEntries.sort(java.util.Map.Entry.<Character, Integer>comparingByValue().reversed());

            StringBuilder result = new StringBuilder();
            result.append("文本处理结果:\n\n");
            result.append("1. 文本统计:\n");
            result.append("   - 总字符数: ").append(totalLength).append("\n");
            result.append("   - 非空格字符数: ").append(nonSpaceLength).append("\n");
            result.append("   - 单词数: ").append(wordCount).append("\n");
            result.append("   - 总行数: ").append(lineCount).append("\n");
            result.append("\n2. 文本摘要 (前").append(summaryLength).append("个字符):\n");
            result.append("   ").append(summary).append("\n");
            result.append("\n3. 字符频率统计 (前5个最常见字符):\n");

            int charLimit = Math.min(5, sortedEntries.size());
            for (int i = 0; i < charLimit; i++) {
                java.util.Map.Entry<Character, Integer> entry = sortedEntries.get(i);
                result.append("   - '").append(entry.getKey()).append("': ").append(entry.getValue()).append(" 次\n");
            }

            return result.toString();
        } catch (Exception e) {
            log.error("文本处理失败", e);
            throw new ConfluenceException("文本处理失败: " + e.getMessage(), e);
        }
    }

    // ==================== 私有方法 ====================

    private String parseSearchResultsWithContent(String jsonResponse, int targetLimit) {
        try {
            // 检查是否为空结果
            if (jsonResponse.contains("\"size\":0") || jsonResponse.contains("\"results\":[]")) {
                return "未找到相关内容";
            }

            // 使用JsonParserUtil解析搜索结果
            java.util.List<JsonParserUtil.ConfluencePage> pages;
            try {
                pages = jsonParserUtil.parseSearchResults(jsonResponse);
            } catch (JsonParserUtil.ConfluenceJsonParseException e) {
                log.warn("JSON解析失败，尝试正则兜底: {}", e.getMessage());
                pages = parseSearchResultsWithRegex(jsonResponse);
            }

            if (pages == null || pages.isEmpty()) {
                return "未找到相关内容";
            }

            int displayCount = Math.min(pages.size(), targetLimit);

            StringBuilder result = new StringBuilder();
            result.append(String.format("搜索完成，找到 %d 个相关内容:\n\n", pages.size()));

            for (int i = 0; i < displayCount; i++) {
                JsonParserUtil.ConfluencePage page = pages.get(i);
                result.append("=== ").append(page.getTitle()).append(" ===\n");
                result.append("- 页面ID: ").append(page.getId()).append("\n");
                result.append("- 访问链接: ").append(config.getUrl()).append(page.getWebUrl()).append("\n");
                result.append("- 所在空间: ").append(page.getSpaceKey()).append("\n");

                // 优先使用excerpt，N+1问题：不再对每个结果额外请求一次
                if (StringUtils.hasText(page.getExcerpt())) {
                    String cleanExcerpt = Jsoup.parse(page.getExcerpt()).text();
                    result.append("- 内容摘要: ").append(cleanExcerpt).append("\n");
                }

                result.append("\n");
            }

            if (displayCount < pages.size()) {
                result.append(String.format("(仅显示前 %d 条结果，共找到 %d 条)\n", displayCount, pages.size()));
            }

            return result.toString();
        } catch (Exception e) {
            log.error("解析搜索结果失败", e);
            throw new ConfluenceException("解析搜索结果失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用正则表达式兜底解析搜索结果（当Jackson解析失败时）
     */
    private java.util.List<JsonParserUtil.ConfluencePage> parseSearchResultsWithRegex(String jsonResponse) {
        java.util.List<JsonParserUtil.ConfluencePage> pages = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"results\":\\[(.*?)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);

        if (!matcher.find()) {
            return pages;
        }

        String resultsSection = matcher.group(1);
        java.util.regex.Pattern pagePattern = java.util.regex.Pattern.compile(
                "\"id\":\"([^\"]+)\".*?\"title\":\"([^\"]+)\".*?\"_links\":\\{\"webui\":\"([^\"]+)\".*?\"key\":\"([^\"]+)\"");
        java.util.regex.Matcher pageMatcher = pagePattern.matcher(resultsSection);

        while (pageMatcher.find()) {
            JsonParserUtil.ConfluencePage page = new JsonParserUtil.ConfluencePage();
            page.setId(pageMatcher.group(1));
            page.setTitle(pageMatcher.group(2));
            page.setWebUrl(pageMatcher.group(3));
            page.setSpaceKey(pageMatcher.group(4));
            pages.add(page);
        }
        return pages;
    }

    /**
     * 构建CQL查询字符串
     */
    private String buildCqlQuery(String searchKeyword, String space, String contentType) {
        // 规范化contentType：去空格，按逗号分割转CQL IN语法
        String[] types = contentType.split(",");
        String typePart = String.join(", ", java.util.Arrays.stream(types)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> "\"" + s + "\"")
                .toArray(String[]::new));

        return String.format("siteSearch ~ \"%s\" AND space in (\"%s\") AND type in (%s)",
                searchKeyword, space, typePart);
    }

    /**
     * 构建搜索URL
     */
    private String buildSearchUrl(String cql, int limit) {
        String queryParams = String.format(
                "cql=%s&start=0&limit=%d&excerpt=enabled&includeArchivedSpaces=false&src=next.ui.search",
                URLEncoder.encode(cql, StandardCharsets.UTF_8),
                limit
        );
        return config.getUrl() + "/rest/api/search?" + queryParams;
    }

    /**
     * 构建页面REST API URL（获取JSON格式内容）
     */
    private String buildPageRestApiUrl(String pageId) {
        return config.getUrl() + "/rest/api/content/" + pageId + "?expand=body.view";
    }

    /**
     * 构建页面HTML渲染URL（兜底用）
     */
    private String buildPageUrl(String pageId) {
        return config.getUrl() + "/pages/viewpage.action?pageId=" + pageId;
    }

    private String extractMainContentFromHtml(Document doc) {
        try {
            StringBuilder content = new StringBuilder();
            Elements contentElements = doc.select("div.wiki-content, div#main-content, div#content");

            if (contentElements.isEmpty()) {
                Element body = doc.body();
                if (body != null) {
                    body.select("nav, header, footer, .aui-header, .ia-splitter-left, #navigation").remove();
                    contentElements = new Elements(body);
                }
            }

            for (Element element : contentElements) {
                String text = element.text();
                if (text != null && !text.trim().isEmpty()) {
                    if (content.length() > 0) {
                        content.append("\n\n");
                    }
                    content.append(text);
                }
            }

            if (content.length() > 0) {
                String result = content.toString().replaceAll("\\s+", " ").trim();
                if (result.length() > FULL_CONTENT_MAX_LENGTH) {
                    result = result.substring(0, FULL_CONTENT_MAX_LENGTH) + "...(内容过长，已截断)";
                }
                return result;
            }
            return null;
        } catch (Exception e) {
            log.error("提取HTML主要内容失败", e);
            return null;
        }
    }
}
