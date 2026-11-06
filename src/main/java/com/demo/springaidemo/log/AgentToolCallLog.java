package com.demo.springaidemo.log;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolCallLog {
    private String sessionId;
    private String role;
    private String toolClassName;
    private String methodName;
    private String params;
    private String result;
    private LocalDateTime callTime;
//只有 getter，没有 setter：日志一旦生成，数据不允许修改，只读。
    public String getSessionId() {
        return sessionId;
    }

    public String getRole() {
        return role;
    }

    public String getToolClassName() {
        return toolClassName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getParams() {
        return params;
    }

    public String getResult() {
        return result;
    }

    public LocalDateTime getCallTime() {
        return callTime;
    }
}
