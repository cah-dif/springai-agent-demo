package com.demo.springaidemo.registry;

import com.demo.springaidemo.anno.AgentTool;
import com.demo.springaidemo.context.AgentSessionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private final Map<String, Object> allToolMap = new HashMap<>();
    private final Map<String, Set<String>> toolPermissionMap = new HashMap<>();

    public ToolRegistry(ApplicationContext context) {
        Map<String, Object> toolBeans = context.getBeansWithAnnotation(AgentTool.class);
        toolBeans.forEach((beanName, beanObj) -> {
            AgentTool anno = beanObj.getClass().getAnnotation(AgentTool.class);
            allToolMap.put(beanName, beanObj);
            toolPermissionMap.put(beanName, new HashSet<>(Arrays.asList(anno.permission())));
        });
        System.out.println("==== 扫描加载Agent工具完成，工具总数：" + allToolMap.size() + " ====");
    }

    public Map<String, Object> getToolsByRole(AgentSessionContext context) {
        String role = context.getRole();
        return allToolMap.entrySet().stream()
                .filter(entry -> {
                    Set<String> perms = toolPermissionMap.get(entry.getKey());
                    return perms.isEmpty() || perms.contains(role);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
