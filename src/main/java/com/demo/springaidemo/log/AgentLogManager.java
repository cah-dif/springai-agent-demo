package com.demo.springaidemo.log;

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

    // 清空日志
    public void clear(){
        logList.clear();
    }
}
