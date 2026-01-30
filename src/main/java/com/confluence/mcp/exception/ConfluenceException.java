package com.confluence.mcp.exception;

/**
 * Confluence业务异常类
 */
public class ConfluenceException extends RuntimeException {

    public ConfluenceException(String message) {
        super(message);
    }

    public ConfluenceException(String message, Throwable cause) {
        super(message, cause);
    }
}