package com.demo.springaidemo.context;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

public class AgentSessionContext {
    // 会话id，前端传过来，区分不同聊天窗口
    private String sessionId;
    // 用户身份有user / admin
    private String role;

    // 新增：本会话的历史对话（多轮记忆），只存 user / assistant 两种消息
    private final List<Message> history = new ArrayList<>();

    // 最多保留多少条消息，一问一答算 2 条；超过就把最老的丢掉，防止上下文无限变长
    private static final int MAX_MESSAGES = 10;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /** 取一份历史消息的副本，外面改不动内部集合 */
    public List<Message> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    /** 一轮问答结束后把「用户问了什么 + 模型答了什么」存进历史，超上限从头裁掉 */
    public void addTurn(String question, String answer) {
        synchronized (history) {
            history.add(new UserMessage(question));
            history.add(new AssistantMessage(answer));
            while (history.size() > MAX_MESSAGES) {
                history.remove(0);
            }
        }
    }

    /** 清空本会话的记忆 */
    public void clearHistory() {
        synchronized (history) {
            history.clear();
        }
    }

    /** 历史消息条数，一问一答算 2 条 */
    public int getHistorySize() {
        synchronized (history) {
            return history.size();
        }
    }
}
