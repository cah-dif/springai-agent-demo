package com.demo.springaidemo.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data

public class AgentToolCallLog {
    private String sessionId;
    private String role;
    private String question;
    private String toolClassName;
    private String methodName;
    private String params;
    private String result;//工具返回结果
    private String answer;//大模型最终返回结果
    private LocalDateTime callTime;
}
