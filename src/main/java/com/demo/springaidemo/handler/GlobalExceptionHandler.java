package com.demo.springaidemo.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：新增文件，没有改动原有 Controller 的逻辑
 * 作用：Controller 里抛出的异常不再直接把堆栈丢给前端，统一转成一句人能看懂的话
 * 只拦截 controller 包，避免影响别的组件
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.demo.springaidemo.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        // 控制台照旧打印完整日志，方便自己排查
        log.error("接口执行异常：{}", e.getMessage(), e);
        String msg = buildFriendlyMessage(e);
        // 用纯文本返回，前端 chat.html 现在就是直接展示返回文本，不用改
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8))
                .body(msg);
    }

    /** 缺少必填参数时也给一句人话，而不是报错堆栈 */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParam(
            org.springframework.web.bind.MissingServletRequestParameterException e) {
        log.warn("缺少必填参数：{}", e.getParameterName());
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8))
                .body("缺少必要的参数【" + e.getParameterName() + "】，请重新输入");
    }

    /** 把技术异常翻译成普通用户能看懂的话 */
    private String buildFriendlyMessage(Exception e) {
        String origin = e.getMessage() == null ? "" : e.getMessage();
        String lower = origin.toLowerCase();

        if (lower.contains("401") || lower.contains("unauthorized") || lower.contains("api key")) {
            return "AI 服务调用失败：API Key 无效或未配置，请检查 DEEPSEEK_KEY 环境变量";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "AI 服务调用超时，请稍后重试";
        }
        if (lower.contains("429") || lower.contains("rate limit")) {
            return "AI 服务调用过于频繁，请稍后再试";
        }
        if (lower.contains("connect") || lower.contains("unknownhost") || lower.contains("network")) {
            return "网络异常：无法连接到 AI 服务，请检查网络后重试";
        }
        if (e instanceof IllegalArgumentException) {
            return "请求参数不合法：" + origin;
        }
        return "系统处理失败，请稍后重试（原因：" + origin + "）";
    }
}
