package com.confluence.mcp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * HTTP客户端配置类
 * 配置可复用连接池和带超时感知的HttpClient
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final ConfluenceConfig confluenceConfig;

    /**
     * 配置HTTP连接池，从ConfluenceConfig读取参数
     */
    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create();

        try {
            SSLContext sslContext = SSLContext.getDefault();
            builder.setSSLSocketFactory(
                    SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .build()
            );
        } catch (Exception e) {
            log.warn("初始化SSL上下文失败，使用默认配置: {}", e.getMessage());
        }

        PoolingHttpClientConnectionManager manager = builder
                .setMaxConnTotal(confluenceConfig.getMaxPoolSize())
                .setMaxConnPerRoute(confluenceConfig.getMaxPoolPerRoute())
                .build();

        log.info("HTTP连接池初始化完成: maxTotal={}, maxPerRoute={}",
                confluenceConfig.getMaxPoolSize(), confluenceConfig.getMaxPoolPerRoute());

        return manager;
    }

    /**
     * 配置HTTP客户端，设置默认请求超时
     */
    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(confluenceConfig.getConnectionTimeout()))
                .setResponseTimeout(Timeout.ofMilliseconds(confluenceConfig.getReadTimeout()))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(defaultRequestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(Timeout.ofSeconds(30))
                .build();
    }
}
