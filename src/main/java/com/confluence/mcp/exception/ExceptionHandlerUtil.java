package com.confluence.mcp.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice
public class ExceptionHandlerUtil {

    /**
     * 处理Confluence业务异常
     */
    @ExceptionHandler(ConfluenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleConfluenceException(ConfluenceException e) {
        log.warn("Confluence业务异常: {}", e.getMessage());
        return e.getMessage();
    }

    /**
     * 处理其他运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleRuntimeException(RuntimeException e) {
        log.error("系统运行时异常", e);
        return "系统内部错误，请稍后重试";
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleException(Exception e) {
        log.error("系统异常", e);
        return "系统内部错误，请稍后重试";
    }
}