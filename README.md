# spring-ai-demo —— 带角色权限控制的 AI Agent 工具调用系统

基于 Spring Boot 3.3 + Spring AI 1.0 + DeepSeek 实现的 AI Agent 示例项目。

核心特点：**用自定义注解 `@AgentTool` 声明工具，在工具注册阶段按用户角色（role）动态过滤工具集合**，实现「不同身份能用的工具不一样」的权限控制；同时提供工具调用审计日志、待办数据的数据库持久化和统一的异常处理。

一句话概括：**Agent = 大模型 + 工具 + 框架**。大模型负责判断该不该调用工具，工具是真正的 Java 方法，Spring AI 负责把两者连起来。

---

## 一、技术栈

| 技术 | 版本 / 说明 |
| --- | --- |
| JDK | 17 |
| Spring Boot | 3.3.0 |
| Spring AI | 1.0.0（`spring-ai-starter-model-openai`） |
| 大模型 | DeepSeek（通过 OpenAI 兼容接口调用，`deepseek-chat`） |
| Spring Data JPA | 操作数据库，免写 SQL |
| H2 | 2.2.x，文件模式，数据存本地不丢 |
| Lombok | 简化实体类 |
| 前端 | 原生 HTML + CSS + JavaScript（静态页面） |

---

## 二、功能特性

**1. AI 对话与工具调用（支持多轮记忆）**

按 `sessionId` 记住本会话的历史对话：同一个 sessionId 连续提问时，会把前几轮「用户提问 + AI 回答」一起发给模型，所以能连续追问；换个 sessionId 就是一段全新的对话。历史超过 10 条消息时自动裁掉最老的，避免上下文无限变长、token 越滚越多。

单轮内部仍由大模型自行判断是否需要调用工具，调用后把结果整理成自然语言返回。

| 工具名 | 功能 | 所属类 |
| --- | --- | --- |
| `calc_add` | 两数相加 | `CalcTools` |
| `time_getCurrentTime` | 获取当前系统时间 | `TimeTools` |
| `weather_getWeather` | 按城市查询实时天气 | `WeatherTool` |
| `todo_add` | 新增待办 | `TodoTools` |
| `todo_list` | 查询待办列表 | `TodoTools` |
| `todo_finish` | 把指定待办标记为完成 | `TodoTools` |
| `todo_delete` | 删除指定待办 | `TodoTools` |

**2. 角色权限控制**

- `admin`：拥有全部工具
- `user`：拥有计算和待办类工具，没有时间和天气工具

工具是**按请求动态下发**的，没权限的工具模型根本看不到，属于物理隔离，不是靠提示词约束模型自觉。

**3. 待办数据持久化**

待办通过 JPA 写入 H2 文件数据库，**应用重启数据不丢失**。

**4. 工具调用审计日志**

记录会话 ID、角色、提问、调用的工具类与方法、入参、工具返回结果、AI 最终回答、调用时间，并在 `log.html` 页面可视化展示。

**5. 输入校验与统一异常处理**

- 前端拦截空输入、超长输入，不发请求；
- 后端接口入口二次校验；
- 工具内部捕获业务异常（如编号不存在）；
- 全局异常处理把密钥错误、请求超时、限流、网络异常统一翻译成中文提示，不向前端抛堆栈。

**6. 三个前端页面**

`chat.html` 聊天页、`log.html` 审计日志页、H2 数据库控制台（框架自带）。

---

## 三、角色权限矩阵

| 工具 | admin | user |
| --- | --- | --- |
| `calc_add`（加法计算） | ✅ | ✅ |
| `todo_add` / `todo_list` / `todo_finish` / `todo_delete`（待办） | ✅ | ✅ |
| `weather_getWeather`（天气查询） | ✅ | ❌ |
| `time_getCurrentTime`（当前时间） | ✅ | ❌ |

> 权限声明写在工具类上：
> ```java
> @AgentTool(group = "basic", permission = {"admin"})          // 仅 admin 可用
> @AgentTool(group = "basic", permission = {"user", "admin"})  // 两个角色都可用
> @AgentTool(group = "todo",  permission = {"user", "admin"})  // 待办工具
> ```

---

## 四、项目结构

```
src/main/java/com/demo/springaidemo
├── anno/AgentTool.java                  // 自定义注解：声明工具分组与可访问角色
├── config/HttpConfig.java               // RestTemplate 配置（天气工具用）
├── context/
│   ├── AgentSessionContext.java         // 会话上下文：sessionId + role + 历史消息（多轮记忆）
│   └── AgentLogContext.java             // ThreadLocal，把当前日志对象透传给工具方法
├── memory/SessionMemoryManager.java     // 按 sessionId 管理上下文对象，实现会话隔离与记忆复用
├── controller/ChatController.java       // 接口层：/chat/ask、/chat/logs、/chat/memory
├── entity/Todo.java                     // 待办实体，映射 todo 表
├── repository/TodoRepository.java       // 继承 JpaRepository，自带增删改查
├── handler/GlobalExceptionHandler.java  // 全局异常处理，统一转中文提示
├── log/
│   ├── AgentLogManager.java             // 内存日志存储（CopyOnWriteArrayList）
│   └── AgentToolCallLog.java            // 日志实体
├── registry/ToolRegistry.java           // 核心：扫描 @AgentTool 工具并按角色过滤
├── tool/
│   ├── CalcTools.java                   // 加法工具
│   ├── TimeTools.java                   // 时间工具（仅 admin）
│   ├── WeatherTool.java                 // 天气工具（仅 admin）
│   └── TodoTools.java                   // 待办工具（增/查/完成/删，读写数据库）
└── SpringAiDemoApplication.java         // 启动类

src/main/resources
├── application.yml                      // 本地配置（含密钥，已 gitignore，不上传）
├── application-example.yml              // 配置模板（占位符，可上传）
└── static/
    ├── chat.html                        // 聊天页面
    └── log.html                         // 审计日志页面

data/todo_db.mv.db                       // H2 数据库文件（首次启动自动生成，不应提交）
```

---

## 五、快速开始

### 1. 环境准备

- JDK 17（**注意 `JAVA_HOME` 必须指向 17**，指向 JDK 8 会导致编译失败）
- Maven 3.6+（或直接用项目自带的 `mvnw`）
- 一个 DeepSeek API Key（[platform.deepseek.com](https://platform.deepseek.com) 申请）

### 2. 配置密钥

推荐用环境变量，避免密钥进版本库：

```bash
# Windows PowerShell
$env:DEEPSEEK_KEY="你的Key"
```

也可以直接在 `application.yml` 里写：

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

> ⚠️ `application.yml` 已在 `.gitignore` 中，不会被提交到仓库。**不要把真实 Key 推上 GitHub**；如果不小心提交过，`git rm --cached` 只会取消追踪，历史提交里仍然存在，必须到平台吊销并重新生成。

### 3. 数据库配置

项目使用 H2 文件模式，无需安装数据库。以下配置已在 `application.yml` 中，`application-example.yml` 里没有，复制模板时需自行补上：

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/todo_db;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: update      # 启动自动建表/补字段，不清数据
    show-sql: true
```

首启动会自动创建 `data/todo_db.mv.db` 和 `todo` 表，不用手动建表。

### 4. 启动

```bash
mvn spring-boot:run
```

或直接在 IDEA 中运行 `SpringAiDemoApplication`。

### 5. 访问页面

- 聊天页：http://localhost:8080/chat.html
- 日志页：http://localhost:8080/log.html
- 数据库控制台：http://localhost:8080/h2-console
  （JDBC URL 填 `jdbc:h2:file:./data/todo_db`，用户名 `sa`，密码留空）

---

## 六、接口说明

### 1. AI 对话

```
GET /chat/ask?question={问题}&role={admin|user}&sessionId={会话ID}
```

示例：

```
GET /chat/ask?question=1加2等于几&role=user&sessionId=demo001
```

返回：大模型的文字回答（纯文本）。

校验规则：问题为空或超过 300 字符时直接返回提示文本，不调用模型。

### 2. 查询审计日志

```
GET /chat/logs                   // 查询全部
GET /chat/logs?sessionId=demo001 // 按会话查询
```

返回 JSON 数组，字段为：`sessionId`、`role`、`question`、`toolClassName`、`methodName`、`params`、`result`、`answer`、`callTime`。

### 3. 会话记忆

```
GET /chat/memory?sessionId=demo001        // 查看某会话记住了多少轮
GET /chat/memory/clear?sessionId=demo001  // 清空某会话的记忆
```

`/chat/memory` 返回 JSON：

```json
{ "sessionId": "demo001", "historyMessages": 4, "rounds": 2, "sessionCount": 1 }
```

`historyMessages` 是历史消息条数（一问一答算 2 条），`rounds` 是轮数，`sessionCount` 是当前内存里的会话总数。

---

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

三个元注解的作用：

- `@Target(TYPE)`：只能标在类上，标在方法上会编译报错；
- `@Retention(RUNTIME)`：保留到运行时。`SOURCE` 编译后丢弃，`CLASS` 存进字节码但不进运行时内存，两者都无法反射读取权限；
- `@Documented`：生成 Javadoc 时保留该注解。

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

> 设计要点：**工具是"按请求"传给大模型的，不是启动时固定死**。所以同一个应用里，admin 和 user 看到的工具集合不同，这是本项目权限控制的关键。

### 3. 调用链

```
前端 chat.html
   ↓ /chat/ask?question=...&role=...
ChatController（参数校验 → 按 sessionId 取会话上下文（含历史消息）→ 建日志对象 → ThreadLocal.set）
   ↓ toolRegistry.getToolsByRole(context)      // 按角色过滤工具
   ↓ chatClient.prompt().messages(历史消息 + 本次提问).tools(tools)   // 历史与提问一起发；工具挂在本次请求上
DeepSeek 判断是否需要调用工具
   ↓ 需要 → 通过反射回调本地 Java 方法
CalcTools / TimeTools / WeatherTool / TodoTools 执行
   ↓ 执行时通过 AgentLogContext(ThreadLocal) 把工具信息写入日志
   ↓ TodoTools 通过 TodoRepository 读写 H2 数据库
返回 AI 最终回答 → 前端
   ↓ 回答非空时写入该会话的历史消息（多轮记忆）
   ↓ finally：AgentLogContext.clear()
```

### 4. 审计日志的实现思路

- 请求进来时，Controller 创建一条 `AgentToolCallLog`；
- 用 `AgentLogContext.set(log)` 把它塞进 **ThreadLocal**（一次请求全程在同一线程执行）；
- 工具方法执行时 `AgentLogContext.get()` 取出这条日志，填上"我是哪个类、哪个方法、入参是什么、返回了什么"；
- 请求结束在 `finally` 里 `AgentLogContext.clear()`，避免线程复用导致数据串到下一个请求。

> 为什么不只在 Controller 里记？因为 Controller 只知道"用户问了什么、AI 答了什么"，**不知道中间到底调用了哪个工具**。
> 为什么必须 clear？Tomcat 线程是复用的，不清会有两个后果：数据串号（A 的日志被写进 B 的记录）、内存泄漏。

### 5. 待办持久化的实现思路

- `Todo` 实体用 JPA 注解映射到 `todo` 表，字段为 `id`、`content`、`finished`、`createTime`；
- `TodoRepository` 继承 `JpaRepository`，不需要写 SQL 就获得 `save`、`findAll`、`findById`、`deleteById`、`existsById`；
- `TodoTools` 的四个工具方法把原来的内存集合操作全部换成仓库调用；
- H2 用 `file:` 模式而不是 `mem:`，数据落盘，重启不丢。

> 为什么选 H2：不需要安装独立数据库服务，一个依赖随程序启动，适合课程项目。
> 局限：并发能力有限，文件模式基本是单机单进程，**不能用于生产环境**。

### 6. 异常处理的四层防线

| 层级 | 位置 | 处理内容 |
| --- | --- | --- |
| 第一层 | `chat.html` | 空输入、超长输入直接拦截，不发请求 |
| 第二层 | `ChatController` | 接口入口二次校验，防止绕过页面直接调接口 |
| 第三层 | 各工具方法内部 | 捕获业务异常，返回可读提示（如"编号不存在"） |
| 第四层 | `GlobalExceptionHandler` | 兜底所有未捕获异常，翻译成中文提示 |

全局处理器用 `@RestControllerAdvice(basePackages = "...controller")` 限定只拦控制器，返回 `text/plain` 纯文本，前端无需改动即可展示。
映射规则示例：401/api key → 密钥无效；timeout → 请求超时；429/rate limit → 调用过于频繁；connect/unknownhost → 网络异常。

### 7. 多轮对话记忆的实现思路

核心就一句话：**同一个 sessionId，拿到同一个上下文对象，历史消息就能累积。**

1. `SessionMemoryManager` 用一个 `ConcurrentHashMap<String, AgentSessionContext>`，key 是 sessionId，value 是这个会话的上下文；
2. 请求进来先 `getOrCreate(sessionId, role)`：有就复用，没有就新建，所以同一会话的历史不会丢；
3. `AgentSessionContext` 内部维护 `List<Message> history`，只存 `UserMessage` 和 `AssistantMessage`；
4. 调模型时把「历史消息 + 本次提问」一起放进 `messages(...)`：
   ```java
   List<Message> messages = new ArrayList<>(context.getHistory());
   messages.add(new UserMessage(question));

   chatClient.prompt()
           .messages(messages)   // 历史 + 本次提问
           .tools(toolsArr)
           .call()
           .content();
   ```
5. 回答成功后 `context.addTurn(question, res)` 把这一轮存进历史，下次提问就能看到。

设计要点：

- **只存成功的一轮**：模型调用抛异常或返回空，就不写历史，避免把失败信息当成"事实"喂给下一轮；
- **窗口限制**：`MAX_MESSAGES = 10`（一问一答 2 条），超过从头裁剪，控制 token 成本；
- **会话隔离**：不同 sessionId 互不影响，`/chat/memory?sessionId=xxx` 能看到某会话记住了几轮；
- **不加 `SystemMessage`**：历史里只放对话，系统提示词每次请求临时拼，避免重复存。

局限（面试常问）：现在历史存在 JVM 内存里，**服务重启就清空，也不支持多实例**。升级方案是把 `history` 序列化后存 Redis，key 形如 `session:chat:{sessionId}`，设置 TTL 自动过期，这样重启不丢、多实例也能共享。

---

## 八、测试用例

| 角色 | 提问 | 预期结果 |
| --- | --- | --- |
| admin | 现在几点 | 调用 `TimeTools.getCurrentTime`，返回当前时间 |
| admin | 1加2等于几 | 调用 `CalcTools.add`，返回 3.0 |
| admin | 南昌天气怎么样 | 调用 `WeatherTool.getWeather`，返回天气文本 |
| admin | 帮我记个待办：明天交作业 | 调用 `TodoTools.addTodo`，写入数据库并返回编号 |
| admin | 看看我的待办 | 调用 `TodoTools.listTodo`，列出数据库中的记录 |
| user | 现在几点 | **不调用**时间工具（该工具未下发），AI 文字回复无法获取 |
| user | 1加2等于几 | 正常调用 `CalcTools.add` |
| — | 空输入 / 超过 300 字 | 被拦截，返回提示文本 |
| — | 错误 API Key | 返回"AI 服务调用失败，请检查密钥配置" |
| — | 同一 sessionId 先问「我叫小明，记住我的名字」，再问「我叫什么」 | 第二轮能答出小明，说明多轮记忆生效 |
| — | 换一个 sessionId 再问「我叫什么」 | 答不出，说明会话之间互不影响 |
| — | 调 `/chat/memory/clear?sessionId=...` 后再问「我叫什么」 | 答不出，说明记忆已清空，可对比演示 |

**多轮记忆验证**：`/chat/memory?sessionId=demo001` 返回的 `rounds` 会随着对话增加；清空后再查变成 0。

**持久化验证**：新增待办 → 打开 H2 控制台执行 `SELECT * FROM TODO;` → 停掉程序重启 → 再查一次，数据仍在。

---

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

6. **`@Entity` 报"找不到符号"，`jakarta.persistence` 包不存在**
   原因：`spring-boot-starter-data-jpa` 和 `h2` 被误写进 `pom.xml` 的 `<dependencyManagement>` 里。那里只是版本管理，**不会真正引入依赖**，运行时的 classpath 上没有 JPA。
   解决：把这两个依赖移到 `<dependencies>` 里。

7. **配置了 H2 却不生效**
   原因：`datasource`、`h2`、`jpa` 三段被错误缩进到了 `spring.ai` 下面，实际挂在 AI 的配置树里，数据源配置根本没被读取。
   解决：把这三段提到 `spring` 层级，与 `ai` 平级。

8. **实体扫描不到、`@Entity` 不生效**
   原因：`Todo` 和 `TodoRepository` 的包名写成了 `com.example.springaiagentdemo.xxx`，与项目实际包名 `com.demo.springaidemo.xxx` 不一致，Spring 的组件扫描根本扫不到。
   解决：包名统一改为 `com.demo.springaidemo`。

9. **H2 依赖只下载了 pom、缺 jar**
   现象：`Cannot access central ... in offline mode`，或下载卡在 `Resolving dependencies`。
   原因：依赖下载过程中断，本地仓库里只有 `.pom` 没有 `.jar`。
   解决：刷新 Maven 重新下载（IDEA 右侧 Maven 面板点刷新，或执行 `mvn -U compile`）。

10. **`JAVA_HOME` 指向 JDK 8 导致编译失败**
    原因：系统里装了多个 JDK，`JAVA_HOME` 指向 1.8，但项目要求 17。
    解决：把 `JAVA_HOME` 改为 JDK 17 的安装目录，或让 IDEA 的 Project SDK 使用 17。

---

## 十、已知不足与后续可优化方向

**已知不足**

- [ ] 一轮对话调用多个工具时，日志对象只有一个，后一次会覆盖前一次
- [ ] 日志保存在内存，重启丢失
- [ ] 接口用 GET，参数暴露在地址栏
- [ ] 角色由前端传入，可被伪造而越权
- [ ] 多轮记忆存在 JVM 内存，重启清空，且不支持多实例部署

**优化方向**

- [ ] 会话记忆升级：历史改存 Redis（`session:chat:{sessionId}` + TTL），重启不丢、多实例共享
- [ ] 日志持久化：内存 List 换成数据库表，重启不丢
- [ ] 每次工具调用生成独立日志记录，并加请求标识关联同一轮对话
- [ ] 接口改为 POST + 请求体，避免参数出现在 URL
- [ ] 权限模型升级：角色从登录态获取，并扩展到「角色 + 工具分组（group）」两级控制
- [ ] 工具调用耗时统计与失败率监控
- [ ] 表结构管理改用 Flyway / Liquibase，替代 `ddl-auto: update`

---

## 十一、License

本项目仅用于学习与课程作业演示。

> （注：部分内容由豆包 AI 生成）
