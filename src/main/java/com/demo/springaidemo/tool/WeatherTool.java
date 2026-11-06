package com.demo.springaidemo.tool;

import com.demo.springaidemo.anno.AgentTool;
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

    @Tool(name = "weather_getWeather",description = "根据城市名称查询实时天气，用户问某个地方天气的时候调用")
    public String getWeather(String city) {
        // 这里用免费公开的简易天气接口做演示
        String url = "https://wttr.in/" + city + "?format=3";
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            return "查询天气失败，暂时无法获取【" + city + "】天气信息";
        }
    }
}
