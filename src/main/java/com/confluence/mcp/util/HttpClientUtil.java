package com.confluence.mcp.util;

import com.confluence.mcp.config.ConfluenceConfig;
import com.confluence.mcp.exception.ConfluenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * HTTP客户端工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpClientUtil {

    private static final int MAX_RESPONSE_SIZE = 10 * 1024 * 1024; // 10MB

    private final CloseableHttpClient httpClient;
    private final ConfluenceConfig config;

    /**
     * 执行HTTP GET请求
     *
     * @param url 请求URL
     * @return UTF-8编码的响应字符串
     * @throws ConfluenceException 当请求失败或响应超时
     */
    public String executeGetRequest(String url) {
        log.info("执行HTTP GET请求: {}", maskSensitiveUrl(url));
        HttpGet request = new HttpGet(url);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Authorization", getBasicAuthHeader());

        org.apache.hc.client5.http.config.RequestConfig requestConfig =
                org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout()))
                        .setResponseTimeout(Timeout.ofMilliseconds(config.getReadTimeout()))
                        .build();
        request.setConfig(requestConfig);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            log.debug("HTTP响应状态码: {}, URL: {}", statusCode, maskSensitiveUrl(url));

            if (statusCode >= 400) {
                String errorBody = extractResponseBodySafely(response.getEntity());
                throw new ConfluenceException(
                        String.format("HTTP请求失败，状态码: %d，响应: %s", statusCode, errorBody));
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new ConfluenceException("HTTP响应实体为空，URL: " + maskSensitiveUrl(url));
            }

            String responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            if (responseString.length() > MAX_RESPONSE_SIZE) {
                log.warn("HTTP响应大小超过限制 {}MB，URL: {}", MAX_RESPONSE_SIZE / 1024 / 1024, maskSensitiveUrl(url));
                throw new ConfluenceException("HTTP响应过大，超过10MB限制");
            }

            log.info("HTTP响应成功，URL: {}, 响应长度: {}字符", maskSensitiveUrl(url), responseString.length());
            log.debug("HTTP响应内容预览: {}",
                    responseString.length() > 200 ? responseString.substring(0, 200) : responseString);

            return responseString;
        } catch (ConfluenceException e) {
            throw e;
        } catch (org.apache.hc.client5.http.ConnectTimeoutException e) {
            log.error("HTTP连接超时，URL: {}, 超时设置: {}ms", maskSensitiveUrl(url), config.getConnectionTimeout());
            throw new ConfluenceException("Confluence连接超时，请检查网络或服务状态", e);
        } catch (org.apache.hc.client5.http.HttpHostConnectException e) {
            log.error("无法连接到Confluence服务器: {}", maskSensitiveUrl(url));
            throw new ConfluenceException("无法连接到Confluence服务器，请检查URL配置和网络", e);
        } catch (org.apache.hc.core5.http.ContentTooLongException e) {
            log.error("HTTP响应内容过长，URL: {}", maskSensitiveUrl(url));
            throw new ConfluenceException("响应内容过长，超过允许的大小限制", e);
        } catch (Exception e) {
            log.error("HTTP请求执行失败，URL: {}", maskSensitiveUrl(url), e);
            throw new ConfluenceException("HTTP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取Basic认证头
     */
    private String getBasicAuthHeader() {
        String auth = config.getUsername() + ":" + config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 安全地提取响应体，避免二次读取时再次触发大小检查
     */
    private String extractResponseBodySafely(HttpEntity entity) {
        if (entity == null) {
            return "";
        }
        try {
            String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            return body.length() > 500 ? body.substring(0, 500) + "..." : body;
        } catch (Exception e) {
            return "(无法读取响应体)";
        }
    }

    /**
     * 对URL中的敏感信息进行脱敏处理（密码参数）
     */
    private String maskSensitiveUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("([?&]password=)[^&]*", "$1***");
    }
}
