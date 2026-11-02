package com.demo.springaidemo.context;

public class AgentSessionContext {
    // 会话id，前端传过来，区分不同聊天窗口
    private String sessionId;
    // 用户身份有user / admin
    private String role;

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
}
