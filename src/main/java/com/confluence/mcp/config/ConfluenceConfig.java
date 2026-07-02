package com.confluence.mcp.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Confluence连接配置属性类
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "confluence")
@Validated
public class ConfluenceConfig {

    /**
     * Confluence服务器地址
     */
    @NotBlank(message = "Confluence URL 不能为空")
    private String url;

    /**
     * 用户名
     */
    @NotBlank(message = "Confluence 用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "Confluence 密码不能为空")
    private String password;

    /**
     * 默认搜索空间
     */
    private String defaultSpace = "ZXJXTECH";

    /**
     * 默认内容类型
     */
    private String defaultContentType = "page,blogpost";

    /**
     * 默认搜索限制数量
     */
    @Positive(message = "默认搜索限制必须为正数")
    @PositiveOrZero(message = "默认搜索限制不能为负数")
    private Integer defaultSearchLimit = 10;

    /**
     * 连接超时时间（毫秒）
     */
    @Positive(message = "连接超时时间必须为正数")
    private Integer connectionTimeout = 30000;

    /**
     * 读取超时时间（毫秒）
     */
    @Positive(message = "读取超时时间必须为正数")
    private Integer readTimeout = 30000;

    /**
     * HTTP连接池最大连接数
     */
    @Positive(message = "最大连接数必须为正数")
    private Integer maxPoolSize = 100;

    /**
     * HTTP连接池每个路由最大连接数
     */
    @Positive(message = "每路由最大连接数必须为正数")
    private Integer maxPoolPerRoute = 20;

    /**
     * 搜索结果最大返回数量
     */
    @Positive(message = "最大搜索数量必须为正数")
    private Integer maxSearchLimit = 100;
}
