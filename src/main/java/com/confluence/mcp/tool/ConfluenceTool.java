package com.confluence.mcp.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class ConfluenceTool {

    @Value("${confluence.url}")
    private String confluenceUrl;

    @Value("${confluence.username}")
    private String confluenceUsername;

    @Value("${confluence.password}")
    private String confluencePassword;

    @Tool(description = "在Confluence中搜索内容，支持按关键字、空间和类型进行搜索，并返回详细的页面内容")
    public String searchConfluence(String searchKeyword, String space, String contentType, Integer limit) {
        log.info("Confluence搜索请求: 关键字={}, 空间={}, 内容类型={}, 限制数={}", searchKeyword, space, contentType, limit);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 设置默认值
            if (space == null || space.isEmpty()) {
                space = "RP";
            }
            if (contentType == null || contentType.isEmpty()) {
                contentType = "page,blogpost";
            }
            if (limit == null) {
                limit = 10;
            }

            // 构建CQL查询
            String cql = String.format("siteSearch ~ \"%s\" AND space in (\"%s\") AND type in (\"%s\")",
                    searchKeyword, space, contentType.replace(",", "\",\""));

            // 构建查询参数
            String queryParams = String.format(
                    "cql=%s&start=0&limit=%d&excerpt=none&expand=space.icon&includeArchivedSpaces=false&src=next.ui.search",
                    URLEncoder.encode(cql, StandardCharsets.UTF_8),
                    limit
            );

            // 构建请求URL
            String url = confluenceUrl + "/rest/api/search?" + queryParams;

            // 创建HTTP请求
            ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.get(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Basic " + Base64.getEncoder()
                            .encodeToString((confluenceUsername + ":" + confluencePassword).getBytes()));

            // 发送请求并处理响应
            try (CloseableHttpResponse response = httpClient.execute(requestBuilder.build())) {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);
                log.info("Confluence搜索响应: {}", responseString);

                // 解析响应并获取页面详细内容
                return parseSearchResultsWithContent(responseString);
            }
        } catch (Exception e) {
            log.error("Confluence搜索失败", e);
            return "搜索失败: " + e.getMessage();
        }
    }

    @Tool(description = "获取Confluence页面内容")
    public String getConfluencePage(String pageId) {
        log.info("获取Confluence页面请求: 页面ID={}", pageId);
        log.info("Confluence URL: {}", confluenceUrl);
        log.info("Username: {}", confluenceUsername);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = confluenceUrl + "/pages/viewpage.action?pageId=" + pageId;
            log.info("Request URL: {}", url);
//            https://wiki.in.eciticcfc.com/pages/viewpage.action?pageId=126338655

            String authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((confluenceUsername + ":" + confluencePassword).getBytes());

            ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.get(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", authHeader);

            try (CloseableHttpResponse response = httpClient.execute(requestBuilder.build())) {
                int statusCode = response.getCode();
                log.info("HTTP Status Code: {}", statusCode);

                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);
                log.info("Confluence页面内容响应前200字符: {}",
                         responseString.length() > 200 ? responseString.substring(0, 200) + "..." : responseString);

                // 检查是否是HTML响应
                if (responseString.contains("<html") || responseString.contains("<!DOCTYPE")) {
                    return extractContentFromHtml(responseString);
                } else {
                    return extractPageContent(responseString);
                }
            }
        } catch (Exception e) {
            log.error("获取Confluence页面失败", e);
            return "获取页面失败: " + e.getMessage();
        }
    }

    private String parseSearchResultsWithContent(String jsonResponse) {
        try {
            // 检查是否为空结果
            if (jsonResponse.contains("\"size\":0") || jsonResponse.contains("\"results\":[]")) {
                return "未找到相关内容";
            }

            // 简单解析JSON获取结果列表
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"results\":\\[(.*?)\\]");
            java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);

            if (!matcher.find()) {
                return "无法解析搜索结果，响应格式: " + (jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
            }

            StringBuilder result = new StringBuilder();
            result.append("搜索完成，找到以下相关内容:\n\n");

            // 使用简单的字符串解析来提取页面信息
            String resultsSection = matcher.group(1);
            java.util.regex.Pattern pagePattern = java.util.regex.Pattern.compile(
                "\"id\":\"(\\d+)\".*?\"title\":\"([^\"]+)\".*?\"_links\":\\{\"webui\":\"([^\"]+)\""
            );
            java.util.regex.Matcher pageMatcher = pagePattern.matcher(resultsSection);

            int count = 0;
            while (pageMatcher.find() && count < 10) {
                String pageId = pageMatcher.group(1);
                String title = pageMatcher.group(2).replace("\\\"", "\"");
                String webuiUrl = pageMatcher.group(3).replace("\\\"", "\"");

                // 获取页面详细内容
                String pageContent = getPageContentForSearch(pageId);

                result.append("=== ").append(title).append(" ===\n");
                result.append("页面ID: ").append(pageId).append("\n");
                result.append("访问链接: ").append(confluenceUrl).append(webuiUrl).append("\n");

                if (pageContent != null && !pageContent.trim().isEmpty()) {
                    result.append("内容摘要:\n").append(pageContent).append("\n");
                } else {
                    result.append("(内容需要登录或无法访问)\n");
                }

                result.append("\n");
                count++;
            }

            if (count == 0) {
                return "找到相关内容但无法解析页面信息，原始响应: " +
                       (jsonResponse.length() > 300 ? jsonResponse.substring(0, 300) + "..." : jsonResponse);
            }

            return result.toString();
        } catch (Exception e) {
            log.error("解析搜索结果失败", e);
            return "解析搜索结果失败: " + e.getMessage() + "\n原始响应: " +
                   (jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
        }
    }

    private String getPageContentForSearch(String pageId) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = confluenceUrl + "/pages/viewpage.action?pageId=" + pageId;

            String authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((confluenceUsername + ":" + confluencePassword).getBytes());

            ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.get(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", authHeader);

            try (CloseableHttpResponse response = httpClient.execute(requestBuilder.build())) {
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity);

                if (responseString.contains("<html") || responseString.contains("<!DOCTYPE")) {
                    return extractContentSummaryFromHtml(responseString);
                } else {
                    return extractContentSummaryFromJson(responseString);
                }
            }
        } catch (Exception e) {
            log.warn("获取页面内容失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractContentSummaryFromHtml(String htmlResponse) {
        try {
            Document doc = Jsoup.parse(htmlResponse);
            StringBuilder summary = new StringBuilder();

            // 提取主要内容
            Elements contentElements = doc.select("div.wiki-content, div#main-content, div#content, .wiki-body");

            if (contentElements.isEmpty()) {
                // 尝试提取body内容并清理
                Element body = doc.body();
                if (body != null) {
                    // 排除导航、页脚等
                    body.select("nav, header, footer, .aui-header, .ia-splitter-left, #navigation").remove();
                    contentElements = new Elements(body);
                }
            }

            for (Element element : contentElements) {
                String text = element.text();
                if (text != null && !text.trim().isEmpty()) {
                    if (summary.length() > 0) {
                        summary.append(" ");
                    }
                    summary.append(text.trim());
                }
            }

            String fullContent = summary.toString().replaceAll("\\s+", " ").trim();
            if (fullContent.length() > 500) {
                return fullContent.substring(0, 500) + "...";
            }
            return fullContent;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractContentSummaryFromJson(String jsonResponse) {
        try {
            // 简单提取JSON中的内容
            if (jsonResponse.contains("\"body\"") && jsonResponse.contains("\"value\"")) {
                int valueIndex = jsonResponse.indexOf("\"value\"");
                int quoteStart = jsonResponse.indexOf("\"", valueIndex + 8);
                if (quoteStart != -1) {
                    int quoteEnd = jsonResponse.indexOf("\"", quoteStart + 1);
                    if (quoteEnd != -1) {
                        String content = jsonResponse.substring(quoteStart + 1, quoteEnd);
                        if (content.length() > 500) {
                            return content.substring(0, 500) + "...";
                        }
                        return content;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPageContent(String jsonResponse) {
        try {
            // 提取页面内容
            if (jsonResponse.contains("\"body\"")) {
                int bodyStart = jsonResponse.indexOf("\"body\"");
                int viewStart = jsonResponse.indexOf("\"view\"", bodyStart);
                int valueStart = jsonResponse.indexOf("\"value\"", viewStart);
                int contentStart = jsonResponse.indexOf("\"", valueStart + 8) + 1;
                int contentEnd = jsonResponse.indexOf("\"", contentStart);

                if (contentStart > 0 && contentEnd > contentStart) {
                    return jsonResponse.substring(contentStart, contentEnd);
                }
            }
            return "无法提取页面内容，响应格式: " + (jsonResponse.length() > 200 ? jsonResponse.substring(0, 200) + "..." : jsonResponse);
        } catch (Exception e) {
            return "页面内容提取失败: " + e.getMessage();
        }
    }

    private String extractContentFromHtml(String htmlResponse) {
        try {
            // 使用Jsoup解析HTML
            Document doc = Jsoup.parse(htmlResponse);

            // 提取标题
            String title = extractTitleFromHtml(doc);

            // 提取页面内容
            String content = extractMainContentFromHtml(doc);

            if (content != null && !content.trim().isEmpty()) {
                return "页面标题: " + title + "\n\n页面内容:\n" + content;
            }

            return "页面标题: " + title + "\n\n(页面内容需要进一步解析)";
        } catch (Exception e) {
            log.error("HTML内容提取失败", e);
            return "HTML内容提取失败: " + e.getMessage();
        }
    }

    private String extractTitleFromHtml(Document doc) {
        try {
            // 使用doc.title()获取标题
            String title = doc.title();
            if (title != null && !title.isEmpty()) {
                return title;
            }
            // 备选方案：查找meta标签中的页面标题
            Elements metaTags = doc.select("meta[name=ajs-page-title]");
            if (!metaTags.isEmpty()) {
                return metaTags.first().attr("content");
            }
            return "未知标题";
        } catch (Exception e) {
            return "标题提取失败";
        }
    }

    private String extractMainContentFromHtml(Document doc) {
        try {
            // 尝试找到主要内容区域 - Confluence的页面内容通常在以下区域：
            // 1. <div class="wiki-content"> - 主要内容区域
            // 2. <div id="main-content"> - 主体内容区域
            // 3. <div id="content"> - 内容区域

            StringBuilder content = new StringBuilder();

            // 尝试多种选择器来提取内容
            Elements contentElements = doc.select("div.wiki-content, div#main-content, div#content");

            if (contentElements.isEmpty()) {
                // 如果找不到特定区域，尝试提取整个body
                Element body = doc.body();
                if (body != null) {
                    // 排除导航、页脚等不需要的部分
                    Elements excludeElements = body.select("nav, header, footer, .aui-header, .ia-splitter-left, #navigation");
                    for (Element el : excludeElements) {
                        el.remove();
                    }
                    // 将body包装成Elements
                    contentElements = new Elements(body);
                }
            }

            for (Element element : contentElements) {
                // 提取纯文本内容
                String text = element.text();
                if (text != null && !text.trim().isEmpty()) {
                    if (content.length() > 0) {
                        content.append("\n\n");
                    }
                    content.append(text);
                }
            }

            if (content.length() > 0) {
                // 清理过多的空白字符
                String result = content.toString().replaceAll("\\s+", " ").trim();
                // 限制返回内容长度
                if (result.length() > 5000) {
                    result = result.substring(0, 5000) + "...(内容过长，已截断)";
                }
                return result;
            }

            return null;
        } catch (Exception e) {
            log.error("提取主要内容失败", e);
            return null;
        }
    }
}