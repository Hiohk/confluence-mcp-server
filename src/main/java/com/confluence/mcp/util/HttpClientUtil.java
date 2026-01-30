package com.confluence.mcp.util;

import com.confluence.mcp.config.ConfluenceConfig;
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

    private final CloseableHttpClient httpClient;
    private final ConfluenceConfig config;

    /**
     * 执行HTTP GET请求
     */
    public String executeGetRequest(String url) throws Exception {
        log.info("执行HTTP GET请求: URL={}", url);
        log.debug("请求详情 - URL: {}, 超时配置: 连接={}ms, 读取={}ms",
                 url, config.getConnectionTimeout(), config.getReadTimeout());

        HttpGet request = new HttpGet(url);
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Authorization", getBasicAuthHeader());
        request.setConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectionTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getReadTimeout()))
                .build());

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            log.info("HTTP响应状态码: {}, URL={}", statusCode, url);
            log.debug("HTTP响应详情 - 状态码: {}, URL: {}", statusCode, url);

            if (statusCode >= 400) {
                log.error("HTTP请求失败，状态码: {}, URL={}", statusCode, url);
                throw new RuntimeException("HTTP请求失败，状态码: " + statusCode);
            }

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            log.info("HTTP响应成功: URL={}, 响应长度={}字符", url, responseString.length());
            log.debug("HTTP响应内容预览: {}...",
                     responseString.length() > 200 ? responseString.substring(0, 200) : responseString);

            return responseString;
        }
    }

    /**
     * 获取Basic认证头
     */
    private String getBasicAuthHeader() {
        String auth = config.getUsername() + ":" + config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}