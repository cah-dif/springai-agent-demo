package com.demo.springaidemo.tool;

import com.demo.springaidemo.anno.AgentTool;
import com.demo.springaidemo.context.AgentLogContext;
import com.demo.springaidemo.log.AgentToolCallLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@AgentTool(group = "basic", permission = {"user", "admin"})
public class CalcTools {

    //这个@Tool要紧紧挨着方法，不要空行
    @Tool(name = "calc_add", description = "计算两个数字相加，用户问a加b等于多少的时候调用。参数a是第一个数字，参数b是第二个数字")
    public double add(double a, double b) {
        AgentToolCallLog l = AgentLogContext.get();
        if (l != null) {
            l.setToolClassName("CalcTools");
            l.setMethodName("add");
            l.setParams("a=" + a + ", b=" + b);
        }
        double r = a + b;
        if (l != null) l.setResult(String.valueOf(r));
        return r;
    }
}
