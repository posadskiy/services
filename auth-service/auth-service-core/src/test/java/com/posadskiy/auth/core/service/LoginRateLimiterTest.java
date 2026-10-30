package com.posadskiy.auth.core.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.RateLimitedException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private static LoginRateLimiter limiter(int perMinute) {
        AuthProperties properties = new AuthProperties(
                null,
                null,
                null,
                new AuthProperties.RateLimit(perMinute, Duration.ofMinutes(1)),
                null,
                null,
                null,
                null);
        return new LoginRateLimiter(properties);
    }

    @Test
    void oneAccountFromOneAddressIsLimited() {
        LoginRateLimiter limiter = limiter(3);
        for (int i = 0; i < 3; i++) {
            limiter.check("1.1.1.1|login|a@example.com");
        }
        assertThrows(RateLimitedException.class, () -> limiter.check("1.1.1.1|login|a@example.com"));
        assertDoesNotThrow(() -> limiter.check("1.1.1.1|login|b@example.com"));
    }

    @Test
    void wrongPasswordsFromManyAddressesEventuallyRequireACaptcha() {
        LoginRateLimiter limiter = limiter(3);
        for (int i = 0; i < LoginRateLimiter.ACCOUNT_FAILURES_BEFORE_CHALLENGE - 1; i++) {
            limiter.recordFailure("victim@example.com");
        }
        assertFalse(limiter.challengeRequired("victim@example.com"));
        limiter.recordFailure("victim@example.com");
        assertTrue(limiter.challengeRequired("victim@example.com"));
        assertFalse(limiter.challengeRequired("other@example.com"));
    }

    @Test
    void aSuccessfulLoginClearsTheAccountFailures() {
        LoginRateLimiter limiter = limiter(3);
        for (int i = 0; i < LoginRateLimiter.ACCOUNT_FAILURES_BEFORE_CHALLENGE; i++) {
            limiter.recordFailure("a@example.com");
        }
        limiter.clearFailures("a@example.com");
        assertFalse(limiter.challengeRequired("a@example.com"));
    }

    @Test
    void oneAddressTryingManyAccountsIsLimited() {
        LoginRateLimiter limiter = limiter(2);
        int allowed = 2 * LoginRateLimiter.ADDRESS_LIMIT_FACTOR;
        for (int i = 0; i < allowed; i++) {
            limiter.checkAddress("2.2.2.2");
            limiter.check("2.2.2.2|login|user" + i + "@example.com");
        }
        assertThrows(RateLimitedException.class, () -> limiter.checkAddress("2.2.2.2"));
        assertDoesNotThrow(() -> limiter.checkAddress("3.3.3.3"));
    }
}
