package com.demo.springaidemo.tool;

import com.demo.springaidemo.anno.AgentTool;
import com.demo.springaidemo.context.AgentLogContext;
import com.demo.springaidemo.entity.Todo;
import com.demo.springaidemo.log.AgentToolCallLog;
import com.demo.springaidemo.repository.TodoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 待办工具：数据存在 H2 文件数据库里，重启程序数据不丢
 * 这是新增的工具类，没有改动 CalcTools / TimeTools / WeatherTool
 */
@Component
@AgentTool(group = "todo", permission = {"user", "admin"})
public class TodoTools {

    private final TodoRepository todoRepository;

    // 构造注入仓库，由 Spring 自动传进来
    public TodoTools(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Tool(name = "todo_add", description = "新增一条待办事项，用户说帮我记一下、加个待办、添加任务的时候调用。参数content是待办的具体内容")
    public String addTodo(@ToolParam(description = "待办的具体内容") String content) {
        AgentToolCallLog l = AgentLogContext.get();
        if (l != null) {
            l.setToolClassName("TodoTools");
            l.setMethodName("addTodo");
            l.setParams("content=" + content);
        }
        String out;
        try {
            if (content == null || content.isBlank()) {
                out = "新增待办失败：待办内容不能为空";
            } else {
                Todo todo = new Todo();
                todo.setContent(content);
                todo.setFinished(false);
                Todo saved = todoRepository.save(todo);
                out = "新增待办成功，编号 " + saved.getId() + "，内容：" + saved.getContent();
            }
        } catch (Exception e) {
            out = "新增待办失败：数据库操作异常，请稍后重试";
        }
        if (l != null) l.setResult(out);
        return out;
    }

    @Tool(name = "todo_list", description = "查询待办列表，用户问看看我的待办、有哪些待办、还有什么事没做的时候调用")
    public String listTodo() {
        AgentToolCallLog l = AgentLogContext.get();
        if (l != null) {
            l.setToolClassName("TodoTools");
            l.setMethodName("listTodo");
            l.setParams("无");
        }
        String out;
        try {
            List<Todo> all = todoRepository.findAll();
            if (all.isEmpty()) {
                out = "当前没有任何待办事项";
            } else {
                out = "共 " + all.size() + " 条待办：" + all.stream()
                        .map(t -> "[" + t.getId() + "] " + t.getContent()
                                + (Boolean.TRUE.equals(t.getFinished()) ? "（已完成）" : "（未完成）"))
                        .collect(Collectors.joining("；"));
            }
        } catch (Exception e) {
            out = "查询待办失败：数据库操作异常，请稍后重试";
        }
        if (l != null) l.setResult(out);
        return out;
    }

    @Tool(name = "todo_finish", description = "把某条待办标记为已完成，用户说这条待办做完了、标记完成的时候调用。参数id是待办编号")
    public String finishTodo(@ToolParam(description = "待办的编号id，数字") Long id) {
        AgentToolCallLog l = AgentLogContext.get();
        if (l != null) {
            l.setToolClassName("TodoTools");
            l.setMethodName("finishTodo");
            l.setParams("id=" + id);
        }
        String out;
        try {
            Optional<Todo> opt = todoRepository.findById(id);
            if (opt.isEmpty()) {
                out = "操作失败：编号 " + id + " 的待办不存在";
            } else {
                Todo todo = opt.get();
                todo.setFinished(true);
                todoRepository.save(todo);
                out = "编号 " + id + " 的待办已标记为完成";
            }
        } catch (Exception e) {
            out = "操作失败：数据库操作异常，请稍后重试";
        }
        if (l != null) l.setResult(out);
        return out;
    }

    @Tool(name = "todo_delete", description = "删除某条待办，用户说删掉这条待办、删除待办的时候调用。参数id是待办编号")
    public String deleteTodo(@ToolParam(description = "待办的编号id，数字") Long id) {
        AgentToolCallLog l = AgentLogContext.get();
        if (l != null) {
            l.setToolClassName("TodoTools");
            l.setMethodName("deleteTodo");
            l.setParams("id=" + id);
        }
        String out;
        try {
            if (!todoRepository.existsById(id)) {
                out = "操作失败：编号 " + id + " 的待办不存在";
            } else {
                todoRepository.deleteById(id);
                out = "编号 " + id + " 的待办已删除";
            }
        } catch (Exception e) {
            out = "操作失败：数据库操作异常，请稍后重试";
        }
        if (l != null) l.setResult(out);
        return out;
    }
}
