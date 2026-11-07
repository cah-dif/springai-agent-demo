package com.demo.springaidemo.controller;

import com.demo.springaidemo.anno.AgentTool;
import com.demo.springaidemo.context.AgentSessionContext;
import com.demo.springaidemo.registry.ToolRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.demo.springaidemo.log.AgentLogManager;
import com.demo.springaidemo.log.AgentToolCallLog;
import java.time.LocalDateTime;
import com.demo.springaidemo.context.AgentSessionContext;



import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
@AllArgsConstructor

public class ChatController {

    private final ChatClient.Builder chatClientBuilder;
    private final ToolRegistry toolRegistry;
    private final AgentLogManager agentLogManager;

    @GetMapping("/ask")
    public String ask(String question, String role, String sessionId) {
        log.info("收到用户提问：{}, 角色：{}, sessionId：{}", question, role, sessionId);

        AgentSessionContext context = new AgentSessionContext();
        context.setRole(role);
        context.setSessionId(sessionId);

        // 构造日志记录，这里先填充会话、角色，工具相关字段先填空字符串预留
         AgentToolCallLog toolLog = new AgentToolCallLog();
         toolLog.setSessionId(sessionId);
         toolLog.setRole(role);
         toolLog.setQuestion(question);
         toolLog.setCallTime(LocalDateTime.now());
         agentLogManager.addLog(toolLog);


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
   /** 查全部调用日志 */
@GetMapping("/logs")
    public Object logs(@RequestParam(required = false) String sessionId) {
    if (sessionId == null || sessionId.isEmpty()) {
        return agentLogManager.getAllLog();
    }
    return agentLogManager.getLogsBySession(sessionId);
}

 }



//这就是业务入口。浏览器访问这个地址，才会触发调用 AI 的逻辑。
//这是整个 Demo 的核心业务代码。