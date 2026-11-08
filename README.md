# spring-ai-demo —— 带角色权限控制的 AI Agent 工具调用系统

基于 Spring Boot 3.3 + Spring AI 1.0 + DeepSeek 实现的 AI Agent 示例项目。核心特点是：**用自定义注解 `@AgentTool` 声明工具，并在工具注册阶段按用户角色（role）动态过滤可用的工具集合**，实现「不同身份能用的工具不一样」的权限控制，同时提供工具调用审计日志。

## 一、技术栈

| 技术 | 版本 / 说明 |
| --- | --- |
| JDK | 17 |
| Spring Boot | 3.3.0 |
| Spring AI | 1.0.0（`spring-ai-starter-model-openai`） |
| 大模型 | DeepSeek（通过 OpenAI 兼容接口调用，`deepseek-chat`） |
| Lombok | 简化实体类 |
| 前端 | 原生 HTML + CSS + JavaScript（静态页面） |

## 二、功能特性

- **AI 对话**：单轮问答，支持大模型自动决定是否调用工具
- **Function Calling 工具**
  - `calc_add` —— 两数相加（CalcTools）
  - `time_getCurrentTime` —— 获取当前系统时间（TimeTools）
  - `weather_getWeather` —— 按城市查询实时天气（WeatherTool）
- **角色权限控制**
  - `admin`：拥有全部工具
  - `user`：没有时间工具，其余可用
- **工具调用审计日志**：记录会话ID、角色、提问、调用的工具类与方法、入参、工具返回结果、AI 最终回答、调用时间
- **两个前端页面**：`chat.html` 聊天页、`log.html` 日志查看页

## 三、角色权限矩阵

| 工具 | admin | user |
| --- | --- | --- |
| calc_add（加法计算） | ✅ | ✅ |
| weather_getWeather（天气查询） | ✅ | ✅ |
| time_getCurrentTime（当前时间） | ✅ | ❌ |

> 权限声明写在工具类上，例如：
> ```java
> @AgentTool(group = "basic", permission = {"admin"})        // 仅 admin 可用
> @AgentTool(group = "basic", permission = {"user", "admin"}) // 两个角色都可用
> ```

## 四、项目结构

```
src/main/java/com/demo/springaidemo
├── anno/AgentTool.java              // 自定义注解：声明工具分组与可访问角色
├── config/HttpConfig.java           // RestTemplate 配置（天气工具用）
├── context/
│   ├── AgentSessionContext.java     // 请求上下文：sessionId + role
│   └── AgentLogContext.java         // ThreadLocal，把当前日志对象透传给工具方法
├── controller/ChatController.java   // 接口层：/chat/ask、/chat/logs
├── log/
│   ├── AgentLogManager.java         // 内存日志存储（CopyOnWriteArrayList）
│   └── AgentToolCallLog.java        // 日志实体
├── registry/ToolRegistry.java       // 核心：扫描 @AgentTool 工具并按角色过滤
├── tool/
│   ├── CalcTools.java               // 加法工具
│   ├── TimeTools.java               // 时间工具（仅 admin）
│   └── WeatherTool.java             // 天气工具
└── SpringAiDemoApplication.java     // 启动类

src/main/resources
├── application.yml                  // 本地配置（含密钥，已 gitignore，不上传）
├── application-example.yml          // 配置模板（占位符，可上传）
└── static/
    ├── chat.html                    // 聊天页面
    └── log.html                     // 审计日志页面
```

## 五、快速开始

### 1. 环境准备
- JDK 17
- Maven 3.6+（或直接用项目自带的 `mvnw`）
- 一个 DeepSeek API Key（[platform.deepseek.com](https://platform.deepseek.com) 申请）

### 2. 配置密钥
把 `application-example.yml` 复制一份，命名为 `application.yml`，填入你自己的 Key：

```yaml
spring:
  ai:
    openai:
      api-key: 你的DeepSeek API Key
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-chat
```

> ⚠️ `application.yml` 已在 `.gitignore` 中，不会被提交到仓库，请勿把真实 Key 推上 GitHub。

### 3. 启动
```bash
mvn spring-boot:run
```
或直接在 IDEA 中运行 `SpringAiDemoApplication`。

### 4. 访问页面
- 聊天页：http://localhost:8080/chat.html
- 日志页：http://localhost:8080/log.html

## 六、接口说明

### 1. AI 对话
```
GET /chat/ask?question={问题}&role={admin|user}&sessionId={会话ID}
```
示例：
```
GET /chat/ask?question=1加2等于几&role=user&sessionId=demo001
```
返回：大模型的文字回答（字符串）。

### 2. 查询审计日志
```
GET /chat/logs                  // 查询全部
GET /chat/logs?sessionId=demo001 // 按会话查询
```
返回 JSON 数组，每条字段为：`sessionId`、`role`、`question`、`toolClassName`、`methodName`、`params`、`result`、`answer`、`callTime`。

## 七、实现原理（核心设计）

### 1. 自定义注解声明工具
```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)   // 必须 RUNTIME，否则反射读不到
@Documented
public @interface AgentTool {
    String group() default "default";
    String[] permission() default {};
}
```

### 2. 启动时扫描 + 请求时按角色过滤（ToolRegistry）
```java
// 启动时：扫描容器里所有带 @AgentTool 的 Bean，把工具对象和权限一起缓存
Map<String, Object> toolBeans = context.getBeansWithAnnotation(AgentTool.class);
```
```java
// 每次请求：只把当前角色有权限的工具交给大模型
return allToolMap.entrySet().stream()
        .filter(e -> {
            Set<String> perms = toolPermissionMap.get(e.getKey());
            return perms.isEmpty() || perms.contains(role);
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

> 设计要点：**工具是"按请求"传给大模型的，不是启动时固定死**。所以同一个应用，admin 和 user 看到的工具集合不同，这是本项目权限控制的关键。

### 3. 调用链
```
前端 chat.html
   ↓ /chat/ask?question=...&role=...
ChatController
   ↓ toolRegistry.getToolsByRole(context)   // 按角色过滤工具
   ↓ chatClient.prompt().user(question).tools(tools)   // 工具挂在本次请求上
DeepSeek 判断是否需要调用工具
   ↓ 需要 → 回调本地 Java 方法
CalcTools / TimeTools / WeatherTool 执行
   ↓ 执行时通过 AgentLogContext(ThreadLocal) 把工具信息写入日志
返回 AI 最终回答 → 前端
```

### 4. 审计日志的实现思路
- 请求进来时，Controller 创建一条 `AgentToolCallLog`；
- 用 `AgentLogContext.set(log)` 把它塞进 **ThreadLocal**（一次请求全程在同一线程执行）；
- 工具方法执行时 `AgentLogContext.get()` 取出这条日志，填上"我是哪个类、哪个方法、入参是什么、返回了什么"；
- 请求结束 `AgentLogContext.clear()`，避免线程复用导致数据串到下一个请求。

> 为什么不只在 Controller 里记？因为 Controller 只知道"用户问了什么、AI 答了什么"，**不知道中间到底调用了哪个工具**。

## 八、测试用例

| 角色 | 提问 | 预期结果 |
| --- | --- | --- |
| admin | 现在几点 | 调用 `TimeTools.getCurrentTime`，返回当前时间 |
| admin | 1加2等于几 | 调用 `CalcTools.add`，返回 3.0 |
| admin | 南昌天气怎么样 | 调用 `WeatherTool.getWeather`，返回天气文本 |
| user | 现在几点 | **不调用**时间工具（该工具未下发），AI 文字回复无法获取 |
| user | 1加2等于几 | 正常调用 `CalcTools.add` |

## 九、踩坑记录（开发过程中真实遇到的问题）

1. **`Multiple tools with the same name` 异常**
   原因：把角色过滤后的工具反复 `defaultTools()` 到**同一个共享的 `ChatClient.Builder`** 上。`defaultTools()` 是**追加**语义，第二次请求工具列表就翻倍，出现同名工具，Spring AI 校验直接抛异常。
   解决：`Builder` 只在构造时 `build()` 一次，工具改为通过 `prompt().tools(...)` **按请求**传入。

2. **日志页面空白、接口 404**
   原因有三：① 前端请求 `/chat/log`，后端实际是 `/chat/logs`（少一个 `s`）；② 前端读取 `item.time`、`item.toolName`，但实体字段叫 `callTime`、`toolClassName`，字段名对不上会全部显示 `undefined`；③ 后端从未给 `toolClassName`、`params` 赋值，所以工具信息永远是空。
   解决：路径对齐、字段名对齐、用 ThreadLocal 在工具方法内补全工具信息。

3. **日志"记不全"**
   只在 Controller 记录，拿不到"调用了哪个工具、入参是什么"。解决方式见第七节第 4 点。

4. **内存日志会丢失**
   日志存在 `CopyOnWriteArrayList` 内存集合里，**应用重启即清空**。并发写用 `ArrayList` 会有线程安全问题，因此改为 `CopyOnWriteArrayList`。

5. **API Key 泄露风险**
   密钥曾写在 `application.yml` 并提交过。处理：加入 `.gitignore`、`git rm --cached` 解除追踪、提供 `application-example.yml` 模板，并到平台**吊销旧 Key 重新生成**。
   > 注意：`git rm --cached` 只能让文件不再被追踪，**不会清除历史提交里的密钥**，所以必须吊销重发。

## 十、后续可优化方向

- [ ] 会话记忆：接入 Redis 保存多轮对话上下文，支持连续追问
- [ ] 日志持久化：内存 List 换成 H2 / MySQL，重启不丢
- [ ] 工具调用耗时统计与失败率监控
- [ ] 权限模型升级：从角色（role）扩展到「角色 + 工具分组（group）」两级控制
- [ ] 前端增加"思考中"加载状态与手动刷新按钮

## 十一、License

本项目仅用于学习与课程作业演示。

> （注：部分内容由豆包 AI 生成）