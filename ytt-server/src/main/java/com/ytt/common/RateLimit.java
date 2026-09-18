package com.ytt.common;

import java.lang.annotation.*;

/**
 * 接口限流注解（内存滑动窗口实现）。
 * 生产环境可替换为 Redis + Lua 的分布式限流。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 时间窗口内允许的最大请求数 */
    int capacity() default 60;

    /** 时间窗口长度（秒） */
    int period() default 60;

    /** 限流维度键（默认同一接口同一 IP 计一次） */
    String key() default "";
}
