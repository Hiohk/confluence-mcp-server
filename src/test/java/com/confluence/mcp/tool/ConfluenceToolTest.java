package com.confluence.mcp.tool;

import com.confluence.mcp.config.ConfluenceConfig;
import com.confluence.mcp.util.HttpClientUtil;
import com.confluence.mcp.util.JsonParserUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

/**
 * ConfluenceTool集成测试（使用Mock HTTP响应）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceTool 集成测试")
class ConfluenceToolTest {

    @Mock
    private ConfluenceConfig config;

    @Mock
    private HttpClientUtil httpClientUtil;

    private JsonParserUtil jsonParserUtil;

    private Validator validator;

    private ConfluenceTool confluenceTool;

    @BeforeEach
    void setUp() {
        jsonParserUtil = new JsonParserUtil(new com.fasterxml.jackson.databind.ObjectMapper());
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        confluenceTool = new ConfluenceTool(config, httpClientUtil, jsonParserUtil, validator);

        when(config.getUrl()).thenReturn("https://wiki.in.eciticcfc.com");
        when(config.getDefaultSpace()).thenReturn("ZXJXTECH");
        when(config.getDefaultContentType()).thenReturn("page,blogpost");
        when(config.getDefaultSearchLimit()).thenReturn(10);
        when(config.getMaxSearchLimit()).thenReturn(100);
    }

    @Test
    @DisplayName("搜索请求应正确构建CQL并发起HTTP请求")
    void testSearchConfluence_BuildsCorrectRequest() {
        String mockSearchResponse = """
                {"results":[{"id":"999","title":"集成测试页面","_links":{"webui":"/pages/viewpage.action?pageId=999"},"space":{"key":"ZXJXTECH"},"excerpt":"测试摘要"}]}""";
        when(httpClientUtil.executeGetRequest(contains("/rest/api/search")))
                .thenReturn(mockSearchResponse);

        String result = confluenceTool.searchConfluence("集成", "ZXJXTECH", "page", 5);

        assertThat(result).isNotNull();
        assertThat(result).contains("集成测试页面");
        assertThat(result).contains("999");
    }

    @Test
    @DisplayName("空搜索结果应返回友好提示")
    void testSearchConfluence_EmptyResults() {
        String emptyResponse = "{\"results\":[]}";
        when(httpClientUtil.executeGetRequest(anyString())).thenReturn(emptyResponse);

        String result = confluenceTool.searchConfluence("不存在的关键字", "ZXJXTECH", "page", 5);

        assertThat(result).isNotNull();
        assertThat(result).contains("未找到");
    }

    @Test
    @DisplayName("获取页面应使用REST API URL")
    void testGetConfluencePage_UsesRestApi() {
        String mockResponse = "{\"body\":{\"view\":{\"value\":\"<p>页面内容</p>\"}}}";
        when(httpClientUtil.executeGetRequest(contains("/rest/api/content/")))
                .thenReturn(mockResponse);

        String result = confluenceTool.getConfluencePage("888");

        assertThat(result).isNotNull();
        assertThat(result).contains("页面内容");
    }

    @Test
    @DisplayName("空关键字应抛出业务异常")
    void testSearchConfluence_BlankKeyword_Throws() {
        assertThatThrownBy(() -> confluenceTool.searchConfluence("  ", null, null, null))
                .hasMessageContaining("关键字");
    }

    @Test
    @DisplayName("空页面ID应抛出业务异常")
    void testGetConfluencePage_BlankId_Throws() {
        assertThatThrownBy(() -> confluenceTool.getConfluencePage("   "))
                .hasMessageContaining("ID");
    }

    @Test
    @DisplayName("processText应正确统计字符频率")
    void testProcessText_CharacterFrequency() {
        String result = confluenceTool.processText("test test Test", null);

        assertThat(result).contains("字符频率统计");
        assertThat(result).contains("'t'");
    }

    @Test
    @DisplayName("processText处理长文本应截断")
    void testProcessText_TruncatesLongContent() {
        String longText = "a".repeat(1000);
        String result = confluenceTool.processText(longText, 100);

        assertThat(result).contains("...");
    }
}
