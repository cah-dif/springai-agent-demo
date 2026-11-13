package com.demo.springaidemo.controller;

import com.demo.springaidemo.context.AgentLogContext;
import com.demo.springaidemo.context.AgentSessionContext;
import com.demo.springaidemo.log.AgentLogManager;
import com.demo.springaidemo.log.AgentToolCallLog;
import com.demo.springaidemo.memory.SessionMemoryManager;
import com.demo.springaidemo.registry.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    // 构造时只 build 一次，之后全局复用，不会再被污染
    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final AgentLogManager agentLogManager;
    // 新增：管每个会话的历史对话，多轮记忆靠它
    private final SessionMemoryManager sessionMemoryManager;

    public ChatController(ChatClient.Builder chatClientBuilder,
                          ToolRegistry toolRegistry,
                          AgentLogManager agentLogManager,
                          SessionMemoryManager sessionMemoryManager) {
        this.chatClient = chatClientBuilder.build();
        this.toolRegistry = toolRegistry;
        this.agentLogManager = agentLogManager;
        this.sessionMemoryManager = sessionMemoryManager;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question,
                      @RequestParam(required = false, defaultValue = "user") String role,
                      @RequestParam(required = false, defaultValue = "default-session") String sessionId) {

        // 新增：入参校验，防止空输入和超长输入直接打到 AI 服务
        if (question == null || question.isBlank()) {
            return "请输入你的问题，内容不能为空";
        }
        if (question.length() > 300) {
            return "输入内容过长，请控制在 300 个字符以内";
        }

        log.info("收到用户提问：{}, 角色：{}, sessionId：{}", question, role, sessionId);

        // 新增：同一个 sessionId 复用同一个上下文，历史对话才能累积（多轮记忆）
        AgentSessionContext context = sessionMemoryManager.getOrCreate(sessionId, role);
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

        // 新增：历史消息 + 本次提问一起发，模型就能看到前面聊过什么
        List<Message> messages = new ArrayList<>(context.getHistory());
        messages.add(new UserMessage(question));

        // 关键：工具绑定在本次请求的 prompt 上，不修改共享的 Builder
        AgentLogContext.set(toolLog);   // ✅ 塞进当前线程
        String res;
        try {
            res = chatClient.prompt()
                    .messages(messages)
                    .tools(toolsArr)
                    .call()
                    .content();
        } finally {
            AgentLogContext.clear();    // ✅ 一定要清，否则线程复用会串数据
        }

        // 新增：回答成功才写入历史，失败（比如网络异常）不污染上下文
        if (res != null && !res.isBlank()) {
            context.addTurn(question, res);
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

    /** 新增：查看某个会话的记忆状态，用来演示"模型记住了几轮" */
    @GetMapping("/memory")
    public Map<String, Object> memory(@RequestParam(required = false, defaultValue = "default-session") String sessionId) {
        AgentSessionContext context = sessionMemoryManager.find(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("historyMessages", context == null ? 0 : context.getHistorySize());
        result.put("rounds", context == null ? 0 : context.getHistorySize() / 2);
        result.put("sessionCount", sessionMemoryManager.size());
        return result;
    }

    /** 新增：清空某个会话的记忆，用来对比"清空前能追问、清空后失忆" */
    @GetMapping("/memory/clear")
    public String clearMemory(@RequestParam(required = false, defaultValue = "default-session") String sessionId) {
        sessionMemoryManager.clear(sessionId);
        return "已清空会话 " + sessionId + " 的对话记忆";
    }
}
