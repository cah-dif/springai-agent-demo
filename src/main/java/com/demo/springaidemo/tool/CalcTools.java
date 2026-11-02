package com.demo.springaidemo.tool;

import com.demo.springaidemo.anno.AgentTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@AgentTool(group = "basic", permission = {"user", "admin"})
public class CalcTools {

    //这个@Tool要紧紧挨着方法，不要空行
    @Tool(description = "计算两个数字相加，用户问a加b等于多少的时候调用。参数a是第一个数字，参数b是第二个数字")
    public double add(double a, double b) {
        return a + b;
    }
}
