package com.confluence.mcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Confluence配置属性类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "confluence")
public class ConfluenceConfig {

    /**
     * Confluence服务器地址
     */
    private String url;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 默认搜索空间
     */
    private String defaultSpace = "RP";

    /**
     * 默认内容类型
     */
    private String defaultContentType = "page,blogpost";

    /**
     * 默认搜索限制数量
     */
    private Integer defaultSearchLimit = 10;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 30000;
}