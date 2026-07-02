package com.confluence.mcp.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器，统一处理应用中抛出的各类异常
 * 注意：此处理器用于处理业务层面的自定义异常，MCP 协议层的异常由 Spring AI MCP 框架自行处理
 */
@Slf4j
@RestControllerAdvice
public class ExceptionHandlerUtil {

    /**
     * 处理Confluence业务异常
     */
    @ExceptionHandler(ConfluenceException.class)
    public ResponseEntity<ErrorResponse> handleConfluenceException(ConfluenceException e) {
        log.warn("Confluence业务异常: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("CONFLUENCE_ERROR", e.getMessage(), null));
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("参数校验失败: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", "参数校验失败", errors));
    }

    /**
     * 错误响应结构
     */
    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
        private Object details;
    }
}
