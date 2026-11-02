package com.demo.springaidemo.anno;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
/*Q：Retention 三个值 SOURCE / CLASS / RUNTIME 区别是什么？这里为什么选 RUNTIME？
A: SOURCE：编译之后注解直接丢掉，字节码里没有；
 CLASS：注解存在 class 字节码，但是 JVM 加载类的时候不加载到内存，反射拿不到；
 RUNTIME：运行时保留，可以通过反射读取注解参数。
 我这里需要 Spring 启动后，反射读取工具类上的 permission 权限，所以必须 RUNTIME。*/
@Documented
public @interface AgentTool {
    // 工具分组，后面用来做权限控制
    String group() default "default";
    // 权限标识：哪些身份可以访问这个工具
    String[] permission() default {};
}
