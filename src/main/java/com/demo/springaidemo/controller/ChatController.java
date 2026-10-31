package com.demo.springaidemo.controller;

import com.demo.springaidemo.tool.CalcTools;
import com.demo.springaidemo.tool.TimeTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j//给 ChatController 加两行日志，打印：用户输入的问题、大模型返回的内容。
@RestController//标记这个类是接口类，返回字符串给浏览器
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;//final修饰的成员变量在定义前必须先初始化

    // ✅ 这里加上 TimeTools timeTools、CalcTools calcTools，让Spring注入它！
    public ChatController(ChatClient.Builder builder, TimeTools timeTools, CalcTools calcTools) {
        this.chatClient = builder
                .defaultTools(timeTools)
                .defaultTools(calcTools)
                .build();
    }


    @GetMapping("/ask")//浏览器用 GET 方式访问 `/chat/ask`
    public String ask(String question){
        log.info("收到用户提问：{}",question);//日志优化
        String res = chatClient.prompt()
                .user(question)
                .call()
                .content();
        log.info("AI返回结果：{}",res);
        return res;
    }
}
//这就是业务入口。浏览器访问这个地址，才会触发调用 AI 的逻辑。
//这是整个 Demo 的核心业务代码。