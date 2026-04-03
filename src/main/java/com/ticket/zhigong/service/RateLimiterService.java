package com.ticket.zhigong.service;

import com.google.common.util.concurrent.RateLimiter;
import com.ticket.zhigong.exception.RateLimitException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    // 10 AI calls per user per minute
    private static final double AI_PERMITS_PER_SECOND = 10.0 / 60.0;
    private final ConcurrentHashMap<Long, RateLimiter> aiLimiters = new ConcurrentHashMap<>();

    // 5 login attempts per IP per minute
    private static final double LOGIN_PERMITS_PER_SECOND = 5.0 / 60.0;
    private final ConcurrentHashMap<String, RateLimiter> loginLimiters = new ConcurrentHashMap<>();

    public void checkRateLimit(Long userId) {
        RateLimiter limiter = aiLimiters.computeIfAbsent(userId,
                id -> RateLimiter.create(AI_PERMITS_PER_SECOND));

        if (!limiter.tryAcquire()) {
            throw new RateLimitException("AI调用过于频繁，请稍后再试（限制：每分钟10次）");
        }
    }

    public void checkLoginRateLimit(String ipAddress) {
        RateLimiter limiter = loginLimiters.computeIfAbsent(ipAddress,
                ip -> RateLimiter.create(LOGIN_PERMITS_PER_SECOND));

        if (!limiter.tryAcquire()) {
            throw new RateLimitException("登录尝试过于频繁，请稍后再试（限制：每分钟5次）");
        }
    }
}
