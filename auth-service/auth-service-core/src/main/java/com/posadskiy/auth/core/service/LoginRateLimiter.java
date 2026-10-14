package com.posadskiy.auth.core.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.RateLimitedException;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class LoginRateLimiter {

    private final Cache<String, AtomicInteger> attempts;
    private final int limit;

    public LoginRateLimiter(AuthProperties properties) {
        this.limit = properties.rateLimit().loginPerMinute();
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(properties.rateLimit().window())
                .maximumSize(100_000)
                .build();
    }

    public void check(String key) {
        AtomicInteger count = attempts.get(key, ignored -> new AtomicInteger());
        if (count.incrementAndGet() > limit) {
            throw new RateLimitedException();
        }
    }
}
