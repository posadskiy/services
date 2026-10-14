package com.posadskiy.auth.core.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;
import java.time.Duration;

/**
 * Typed binding of {@code auth.*} in {@code application.yml}. Values and defaults live in YAML;
 * this type exists so the rest of the code does not read string keys.
 */
@ConfigurationProperties("auth")
public record AuthProperties(Jwt jwt, Password password, Token token, RateLimit rateLimit, OAuth oauth) {

    @ConfigurationProperties("jwt")
    public record Jwt(
            String issuer,
            String audience,
            Duration accessTokenTtl,
            @Nullable String hmacSecret,
            @Nullable String privateKeyPem,
            @Nullable String publicKeyPem) {}

    @ConfigurationProperties("password")
    public record Password(int bcryptCost) {}

    @ConfigurationProperties("token")
    public record Token(Duration refreshTokenTtl) {}

    @ConfigurationProperties("rate-limit")
    public record RateLimit(int loginPerMinute, Duration window) {}

    @ConfigurationProperties("oauth")
    public record OAuth(
            Duration stateTtl,
            Duration loginCodeTtl,
            String redirectBaseUrl,
            @Nullable String frontendRedirectUrl,
            @Nullable String allowedFrontendRedirectUrls) {}
}
