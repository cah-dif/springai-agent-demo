package com.demo.springaidemo.tool;

import com.demo.springaidemo.anno.AgentTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@AgentTool(group = "basic", permission = {"admin"})
public class TimeTools {

    //**Spring AI 的核心注解**，告诉大模型：这是一个可以调用的工具，description 描述非常重要！大模型靠这段文字判断什么时候用这个函数。
    @Tool(description = "获取当前系统时间，当用户问现在几点、当前时间的时候调用这个工具")
    public String getCurrentTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
}
