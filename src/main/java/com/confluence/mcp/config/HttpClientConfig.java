package com.confluence.mcp.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

/**
 * HTTP客户端配置类
 */
@Configuration
public class HttpClientConfig {

    /**
     * 配置HTTP连接池
     */
    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        try {
            SSLContext sslContext = SSLContext.getDefault();

            return PoolingHttpClientConnectionManagerBuilder.create()
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(20)
                    .setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTP connection manager", e);
        }
    }

    /**
     * 配置HTTP客户端
     */
    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager connectionManager) {
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .build();
    }
}