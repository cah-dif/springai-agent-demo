package com.demo.springaidemo.context;

import com.demo.springaidemo.log.AgentToolCallLog;
//把日志挂在线程上
public class AgentLogContext {

    private static final ThreadLocal<AgentToolCallLog> HOLDER = new ThreadLocal<>();

    public static void set(AgentToolCallLog log) { HOLDER.set(log); }

    public static AgentToolCallLog get() { return HOLDER.get(); }

    public static void clear() { HOLDER.remove(); }
}
