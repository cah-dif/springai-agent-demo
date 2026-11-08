package com.demo.springaidemo.controller;

import com.demo.springaidemo.context.AgentLogContext;
import com.demo.springaidemo.context.AgentSessionContext;
import com.demo.springaidemo.log.AgentLogManager;
import com.demo.springaidemo.log.AgentToolCallLog;
import com.demo.springaidemo.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    // 构造时只 build 一次，之后全局复用，不会再被污染
    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final AgentLogManager agentLogManager;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          ToolRegistry toolRegistry,
                          AgentLogManager agentLogManager) {
        this.chatClient = chatClientBuilder.build();
        this.toolRegistry = toolRegistry;
        this.agentLogManager = agentLogManager;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question,
                      @RequestParam(required = false, defaultValue = "user") String role,
                      @RequestParam(required = false, defaultValue = "default-session") String sessionId) {

        log.info("收到用户提问：{}, 角色：{}, sessionId：{}", question, role, sessionId);

        AgentSessionContext context = new AgentSessionContext();
        context.setRole(role);
        context.setSessionId(sessionId);

        AgentToolCallLog toolLog = new AgentToolCallLog();
        toolLog.setSessionId(sessionId);
        toolLog.setRole(role);
        toolLog.setQuestion(question);
        toolLog.setCallTime(LocalDateTime.now());
        agentLogManager.addLog(toolLog);

        Map<String, Object> availableToolBeans = toolRegistry.getToolsByRole(context);
        Object[] toolsArr = availableToolBeans.values().toArray();

        // 关键：工具绑定在本次请求的 prompt 上，不修改共享的 Builder
        AgentLogContext.set(toolLog);   // ✅ 塞进当前线程
        String res;
        try {
            res = chatClient.prompt()
                    .user(question)
                    .tools(toolsArr)
                    .call()
                    .content();
        } finally {
            AgentLogContext.clear();    // ✅ 一定要清，否则线程复用会串数据
        }

        toolLog.setAnswer(res);// ✅ 最终回答单独存
        return res;
    }


    @GetMapping("/logs")
    public Object logs(@RequestParam(required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return agentLogManager.getAllLog();
        }
        return agentLogManager.getLogsBySession(sessionId);
    }
}
