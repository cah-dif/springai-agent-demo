package com.demo.springaidemo.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolCallLog {
    private String sessionId;
    private String role;
    private String question;
    private String toolClassName;
    private String methodName;
    private String params;
    private String result;
    private LocalDateTime callTime;
}
