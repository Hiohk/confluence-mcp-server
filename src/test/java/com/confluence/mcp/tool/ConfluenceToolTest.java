package com.confluence.mcp.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ConfluenceToolTest {

    @Autowired
    private ConfluenceTool confluenceTool;

    @Test
    public void testSearchConfluence() {
        // 测试搜索功能
        String result = confluenceTool.searchConfluence("test", "RP", "page", 5);
        System.out.println("搜索结果: " + result);
    }

    @Test
    public void testGetConfluencePage() {
        // 测试获取页面内容功能
        String result = confluenceTool.getConfluencePage("12345");
        System.out.println("页面内容: " + result);
    }
}