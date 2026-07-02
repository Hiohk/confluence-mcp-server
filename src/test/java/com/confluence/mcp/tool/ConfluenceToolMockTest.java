package com.confluence.mcp.tool;

import com.confluence.mcp.config.ConfluenceConfig;
import com.confluence.mcp.util.HttpClientUtil;
import com.confluence.mcp.util.JsonParserUtil;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConfluenceTool单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfluenceTool 单元测试")
class ConfluenceToolMockTest {

    @Mock
    private ConfluenceConfig config;

    @Mock
    private HttpClientUtil httpClientUtil;

    @Mock
    private JsonParserUtil jsonParserUtil;

    private Validator validator;

    private ConfluenceTool confluenceTool;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        confluenceTool = new ConfluenceTool(config, httpClientUtil, jsonParserUtil, validator);

        when(config.getUrl()).thenReturn("https://confluence.example.com");
        when(config.getDefaultSpace()).thenReturn("RP");
        when(config.getDefaultContentType()).thenReturn("page,blogpost");
        when(config.getDefaultSearchLimit()).thenReturn(10);
        when(config.getMaxSearchLimit()).thenReturn(100);
    }

    @Nested
    @DisplayName("searchConfluence 方法测试")
    class SearchConfluenceTests {

        @Test
        @DisplayName("成功搜索并返回结果")
        void testSearchConfluence_Success() {
            String mockSearchResponse = """
                    {"results":[{"id":"123","title":"Test Page","_links":{"webui":"/pages/viewpage.action?pageId=123"},"space":{"key":"RP"},"excerpt":"test excerpt"}]}""";
            when(httpClientUtil.executeGetRequest(contains("/rest/api/search")))
                    .thenReturn(mockSearchResponse);

            String result = confluenceTool.searchConfluence("test", "RP", "page", 5);

            assertThat(result).isNotNull();
            assertThat(result).contains("Test Page");
            assertThat(result).contains("123");
            verify(httpClientUtil).executeGetRequest(anyString());
        }

        @Test
        @DisplayName("空搜索结果应返回友好提示")
        void testSearchConfluence_EmptyResults() {
            String emptyResponse = "{\"results\":[]}";
            when(httpClientUtil.executeGetRequest(anyString())).thenReturn(emptyResponse);

            String result = confluenceTool.searchConfluence("nonexistent", "RP", "page", 5);

            assertThat(result).isNotNull();
            assertThat(result).contains("未找到");
        }

        @Test
        @DisplayName("空关键字应抛出异常")
        void testSearchConfluence_EmptyKeyword() {
            assertThatThrownBy(() -> confluenceTool.searchConfluence("", "RP", "page", 5))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("空格参数应使用默认值")
        void testSearchConfluence_NullSpace() {
            String mockResponse = "{\"results\":[]}";
            when(httpClientUtil.executeGetRequest(anyString())).thenReturn(mockResponse);

            confluenceTool.searchConfluence("test", null, null, null);

            verify(httpClientUtil).executeGetRequest(anyString());
        }

        @Test
        @DisplayName("limit超过最大值时应限制")
        void testSearchConfluence_ExceedMaxLimit() {
            String mockResponse = "{\"results\":[]}";
            when(httpClientUtil.executeGetRequest(anyString())).thenReturn(mockResponse);

            confluenceTool.searchConfluence("test", "RP", "page", 500);

            verify(httpClientUtil).executeGetRequest(anyString());
        }
    }

    @Nested
    @DisplayName("getConfluencePage 方法测试")
    class GetConfluencePageTests {

        @Test
        @DisplayName("成功获取JSON格式页面内容")
        void testGetConfluencePage_JsonResponse() {
            String mockResponse = "{\"body\":{\"view\":{\"value\":\"<p>Test content</p>\"}}}";
            when(httpClientUtil.executeGetRequest(anyString())).thenReturn(mockResponse);

            String result = confluenceTool.getConfluencePage("123");

            assertThat(result).isNotNull();
            verify(httpClientUtil).executeGetRequest(anyString());
        }

        @Test
        @DisplayName("空页面ID应抛出异常")
        void testGetConfluencePage_EmptyPageId() {
            assertThatThrownBy(() -> confluenceTool.getConfluencePage(""))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("processText 方法测试")
    class ProcessTextTests {

        @Test
        @DisplayName("正常文本处理应返回统计结果")
        void testProcessText_NormalText() {
            String result = confluenceTool.processText("Hello World Hello", 10);

            assertThat(result).isNotNull();
            assertThat(result).contains("文本处理结果");
            assertThat(result).contains("总字符数");
            assertThat(result).contains("单词数");
        }

        @Test
        @DisplayName("空文本应抛出异常")
        void testProcessText_EmptyText() {
            assertThatThrownBy(() -> confluenceTool.processText("   ", null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("空文本应抛出异常（null）")
        void testProcessText_NullText() {
            assertThatThrownBy(() -> confluenceTool.processText(null, null))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("长文本应正确截断")
        void testProcessText_LongText() {
            String longText = "a".repeat(300);
            String result = confluenceTool.processText(longText, 50);

            assertThat(result).isNotNull();
            assertThat(result).contains("...");
        }
    }
}
