package com.demo.springaidemo.controller;

import com.demo.springaidemo.context.AgentSessionContext;
import com.demo.springaidemo.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient.Builder chatClientBuilder;
    private final ToolRegistry toolRegistry;

    public ChatController(ChatClient.Builder chatClientBuilder, ToolRegistry toolRegistry) {
        this.chatClientBuilder = chatClientBuilder;
        this.toolRegistry = toolRegistry;
    }

    @GetMapping("/ask")
    public String ask(String question, String role, String sessionId) {
        log.info("收到用户提问：{}, 角色：{}, sessionId：{}", question, role, sessionId);

        AgentSessionContext context = new AgentSessionContext();
        context.setRole(role);
        context.setSessionId(sessionId);

        Map<String, Object> availableToolBeans = toolRegistry.getToolsByRole(context);
        // values转数组，直接丢给 defaultTools
        Object[] toolsArr = availableToolBeans.values().toArray();

        ChatClient chatClient = chatClientBuilder
                .defaultTools(toolsArr)
                .build();

        String res = chatClient.prompt()
                .user(question)
                .call()
                .content();

        log.info("AI返回结果：{}", res);
        return res;
    }
}

//这就是业务入口。浏览器访问这个地址，才会触发调用 AI 的逻辑。
//这是整个 Demo 的核心业务代码。