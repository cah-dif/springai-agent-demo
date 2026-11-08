package com.demo.springaidemo.tool;

import com.demo.springaidemo.anno.AgentTool;
import com.demo.springaidemo.context.AgentLogContext;
import com.demo.springaidemo.log.AgentToolCallLog;
import lombok.AllArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@AgentTool(group = "basic", permission = {"admin"})
@Component
public class WeatherTool {

    private final RestTemplate restTemplate;

    public WeatherTool(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Tool(name = "weather_getWeather", description = "根据城市名称查询实时天气，用户问某个地方天气的时候调用")
    public String getWeather(String city) {
        AgentToolCallLog l = AgentLogContext.get();
        if (l != null) {
            l.setToolClassName("WeatherTool");
            l.setMethodName("getWeather");
            l.setParams("city=" + city);
        }
        String url = "https://wttr.in/" + city + "?format=3";
        String out;
        try {
            out = restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            out = "查询天气失败，暂时无法获取【" + city + "】天气信息";
        }
        if (l != null) l.setResult(out);
        return out;
    }
}
