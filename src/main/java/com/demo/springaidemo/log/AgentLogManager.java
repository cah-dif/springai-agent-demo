package com.demo.springaidemo.log;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.List;


import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class AgentLogManager {
    // 内存集合存放所有日志
    private final List<AgentToolCallLog> logList = new ArrayList<>();

    // 添加一条日志
    public void addLog(AgentToolCallLog log){
        logList.add(log);
    }

    // 查询全部日志，返回一份新集合，防止外部修改内部日志
    public List<AgentToolCallLog> getAllLog(){
        return new ArrayList<>(logList);
    }

    /** 根据 sessionId 查单次会话的日志 */
    public List<AgentToolCallLog> getLogsBySession(String sessionId) {
        return logList.stream()
                .filter(l -> sessionId.equals(l.getSessionId()))
                .collect(Collectors.toList());
    }

    // 清空日志
    public void clear(){
        logList.clear();
    }
}
