package com.confluence.mcp.tool;

import com.confluence.mcp.config.ConfluenceConfig;
import com.confluence.mcp.util.HttpClientUtil;
import com.confluence.mcp.util.JsonParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * ConfluenceTool单元测试（Mock版本）
 */
class ConfluenceToolMockTest {

    @Mock
    private ConfluenceConfig config;

    @Mock
    private HttpClientUtil httpClientUtil;

    @Mock
    private JsonParserUtil jsonParserUtil;

    @InjectMocks
    private ConfluenceTool confluenceTool;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 设置Mock配置
        when(config.getUrl()).thenReturn("https://confluence.example.com");
        when(config.getDefaultSpace()).thenReturn("RP");
        when(config.getDefaultContentType()).thenReturn("page,blogpost");
        when(config.getDefaultSearchLimit()).thenReturn(10);
    }

    @Test
    void testSearchConfluence_Success() throws Exception {
        // 准备Mock数据 - 搜索响应
        String mockSearchResponse = "{\"results\":[{\"id\":\"123\",\"title\":\"Test Page\",\"_links\":{\"webui\":\"/pages/viewpage.action?pageId=123\"}}]}";

        // 准备Mock数据 - 页面内容响应（空响应，避免重复调用）
        String mockPageResponse = "<html><body>Test content</body></html>";

        // 设置Mock行为
        when(httpClientUtil.executeGetRequest(contains("/rest/api/search"))).thenReturn(mockSearchResponse);
        when(httpClientUtil.executeGetRequest(contains("/pages/viewpage.action"))).thenReturn(mockPageResponse);

        // 执行测试
        confluenceTool.searchConfluence("test", "RP", "page", 5);

        // 验证HTTP调用 - 搜索请求1次，页面内容请求1次（总共2次）
        verify(httpClientUtil, times(2)).executeGetRequest(anyString());
        verify(httpClientUtil).executeGetRequest(contains("/rest/api/search"));
        verify(httpClientUtil).executeGetRequest(contains("/pages/viewpage.action"));
    }

    @Test
    void testGetConfluencePage_JsonResponse() throws Exception {
        // 准备Mock数据
        String mockResponse = "{\"body\":{\"view\":{\"value\":\"Test content\"}}}";
        when(httpClientUtil.executeGetRequest(anyString())).thenReturn(mockResponse);
        when(jsonParserUtil.parsePageContent(anyString())).thenReturn(java.util.Optional.of("Test content"));

        // 执行测试
        confluenceTool.getConfluencePage("123");

        // 验证调用
        verify(httpClientUtil).executeGetRequest(anyString());
        verify(jsonParserUtil).parsePageContent(anyString());
    }

    @Test
    void testGetConfluencePage_HtmlResponse() throws Exception {
        // 准备Mock HTML响应数据
        String mockResponse = "<html><body><div class=\"wiki-content\">Test HTML content</div></body></html>";
        when(httpClientUtil.executeGetRequest(anyString())).thenReturn(mockResponse);

        // 执行测试
        confluenceTool.getConfluencePage("123");

        // 验证HTTP调用
        verify(httpClientUtil).executeGetRequest(anyString());
    }
}