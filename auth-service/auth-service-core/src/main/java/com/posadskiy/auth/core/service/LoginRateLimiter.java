package com.posadskiy.auth.core.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.RateLimitedException;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory limits, which is correct while auth-service runs as a single replica.
 * Two layers: a per-key limit (address and account) and a looser per-address limit, so one address cannot
 * try many different accounts without bound.
 * A third layer counts wrong passwords per account from any address. It never locks the account (that would let
 * anyone lock the owner out); once the count is reached the caller must pass a captcha before the password is tried.
 */
@Singleton
public class LoginRateLimiter {

    static final int ADDRESS_LIMIT_FACTOR = 10;
    static final int ACCOUNT_FAILURES_BEFORE_CHALLENGE = 5;
    static final Duration ACCOUNT_FAILURE_WINDOW = Duration.ofMinutes(15);

    private final Cache<String, AtomicInteger> attempts;
    private final Cache<String, AtomicInteger> addressAttempts;
    private final Cache<String, AtomicInteger> accountFailures;
    private final int limit;

    public LoginRateLimiter(AuthProperties properties) {
        this.limit = properties.rateLimit().loginPerMinute();
        this.attempts = newCache(properties);
        this.addressAttempts = newCache(properties);
        this.accountFailures = Caffeine.newBuilder()
            .expireAfterWrite(ACCOUNT_FAILURE_WINDOW)
            .maximumSize(100_000)
            .build();
    }

    private static Cache<String, AtomicInteger> newCache(AuthProperties properties) {
        return Caffeine.newBuilder()
            .expireAfterWrite(properties.rateLimit().window())
            .maximumSize(100_000)
            .build();
    }

    public void check(String key) {
        count(attempts, key, limit);
    }

    /**
     * Counts every credential-bearing request from one address, whatever account it targets.
     */
    public void checkAddress(String address) {
        count(addressAttempts, address, limit * ADDRESS_LIMIT_FACTOR);
    }

    /**
     * True once this account has had enough wrong passwords, from any address, that the next attempt needs a captcha.
     */
    public boolean challengeRequired(String account) {
        AtomicInteger failures = accountFailures.getIfPresent(account);
        return failures != null && failures.get() >= ACCOUNT_FAILURES_BEFORE_CHALLENGE;
    }

    public void recordFailure(String account) {
        accountFailures.get(account, ignored -> new AtomicInteger()).incrementAndGet();
    }

    public void clearFailures(String account) {
        accountFailures.invalidate(account);
    }

    private static void count(Cache<String, AtomicInteger> cache, String key, int max) {
        AtomicInteger count = cache.get(key, ignored -> new AtomicInteger());
        if (count.incrementAndGet() > max) {
            throw new RateLimitedException();
        }
    }
}
