package com.demo.springaidemo.memory;

import com.demo.springaidemo.context.AgentSessionContext;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作用：同一个 sessionId 反复提问，拿到的是同一个 AgentSessionContext，历史对话就能累积起来
 * 存内存（Map），重启后历史清空
 */
@Component
public class SessionMemoryManager {

    // key = sessionId，value = 这个会话的上下文（含历史消息）
    private final Map<String, AgentSessionContext> sessionMap = new ConcurrentHashMap<>();

    // 会话对象数量上限，防止不同 sessionId 无限堆积把内存撑爆
    private static final int MAX_SESSIONS = 100;

    /**
     * 按 sessionId 取上下文，没有就新建；角色每次请求都可能切，用最新的覆盖
     */
    public AgentSessionContext getOrCreate(String sessionId, String role) {
        String sid = normalize(sessionId);

        // 简单保护：超过上限且是全新会话时，随机淘汰一个旧会话（不是 LRU，课程项目够用）
        if (!sessionMap.containsKey(sid) && sessionMap.size() >= MAX_SESSIONS) {
            Iterator<String> it = sessionMap.keySet().iterator();
            if (it.hasNext()) {
                sessionMap.remove(it.next());
            }
        }

        AgentSessionContext context = sessionMap.computeIfAbsent(sid, k -> {
            AgentSessionContext c = new AgentSessionContext();
            c.setSessionId(k);
            return c;
        });
        context.setRole(role);
        return context;
    }

    /**
     * 查会话，没有返回 null，用于查看记忆状态
     */
    public AgentSessionContext find(String sessionId) {
        return sessionMap.get(normalize(sessionId));
    }

    /**
     * 清空指定会话的记忆，会话对象保留（角色还在）
     */
    public void clear(String sessionId) {
        AgentSessionContext context = sessionMap.get(normalize(sessionId));
        if (context != null) {
            context.clearHistory();
        }
    }

    /**
     * 当前内存里有多少个会话
     */
    public int size() {
        return sessionMap.size();
    }

    /**
     * sessionId 为空时兜底，避免前端没传导致所有请求挤进同一个 key 的 null 分支
     */
    private String normalize(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return "default-session";
        }
        return sessionId.trim();
    }
}
