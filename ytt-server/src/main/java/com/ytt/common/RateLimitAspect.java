package com.ytt.common;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流切面：固定窗口计数器，按「接口 + 客户端IP」维度限流。
 * 单实例内存实现，集群部署时替换为 Redis 实现即可（业务代码零改动）。
 */
@Aspect
@Component
public class RateLimitAspect {

    /** 窗口记录：key -> [窗口起始时间戳, 计数器] */
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 限流倍数：默认为 1（注解 capacity 生效）。
     * dev/test 环境可通过 security.rate-limit.multiplier 放大阈值，便于自动化测试密集调用；
     * 生产环境不配置即为 1，行为不变。
     */
    @Value("${security.rate-limit.multiplier:1}")
    private int multiplier;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(point, rateLimit);
        long now = System.currentTimeMillis();
        long periodMs = rateLimit.period() * 1000L;
        int capacity = rateLimit.capacity() * Math.max(multiplier, 1);

        Window window = windows.compute(key, (k, old) -> {
            if (old == null || now - old.startTime > periodMs) {
                return new Window(now, new AtomicInteger(0));
            }
            return old;
        });

        if (window.counter.incrementAndGet() > capacity) {
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
        return point.proceed();
    }

    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String methodKey = rateLimit.key().isEmpty()
                ? signature.getDeclaringTypeName() + "." + signature.getName()
                : rateLimit.key();
        String ip = "unknown";
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            // DEF-03 修复：限流维度必须使用真实连接 IP，防止伪造 X-Forwarded-For 绕过限流。
            // 若部署在反向代理之后，应由网关层校验并覆盖真实 IP，或扩展白名单逻辑。
            ip = request.getRemoteAddr();
        }
        return methodKey + "@" + ip;
    }

    private static class Window {
        final long startTime;
        final AtomicInteger counter;

        Window(long startTime, AtomicInteger counter) {
            this.startTime = startTime;
            this.counter = counter;
        }
    }
}
