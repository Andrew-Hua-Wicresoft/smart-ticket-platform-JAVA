package com.ticket.zhigong.service;

import com.google.common.util.concurrent.RateLimiter;
import com.ticket.zhigong.exception.RateLimitException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    // 10 AI calls per user per minute
    private static final double PERMITS_PER_SECOND = 10.0 / 60.0;
    private final ConcurrentHashMap<Long, RateLimiter> limiters = new ConcurrentHashMap<>();

    public void checkRateLimit(Long userId) {
        RateLimiter limiter = limiters.computeIfAbsent(userId,
                id -> RateLimiter.create(PERMITS_PER_SECOND));

        if (!limiter.tryAcquire()) {
            throw new RateLimitException("AI调用过于频繁，请稍后再试（限制：每分钟10次）");
        }
    }
}
