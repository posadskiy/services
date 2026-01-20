package com.posadskiy.auth.web.oauth;

import com.posadskiy.auth.core.property.SocialOAuthConfigurationProperties;
import com.posadskiy.auth.web.controller.OAuthController.OAuthTokenResponse;
import jakarta.inject.Singleton;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class OAuthLoginSessionStore {

    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();

    public OAuthLoginSessionStore(SocialOAuthConfigurationProperties configuration) {
        Duration configuredTtl = configuration.getSessionTtl();
        // Default to state TTL or 5 minutes if not provided
        this.ttl = Optional.ofNullable(configuredTtl)
                .or(() -> Optional.ofNullable(configuration.getStateTtl()))
                .orElse(Duration.ofMinutes(5));
    }

    public String create(OAuthTokenResponse response) {
        String code = generateCode();
        StoredSession session = new StoredSession(response, Instant.now().plus(ttl));
        sessions.put(code, session);
        return code;
    }

    public Optional<OAuthTokenResponse> consume(String code) {
        if (code == null) {
            return Optional.empty();
        }
        StoredSession session = sessions.remove(code);
        if (session == null || Instant.now().isAfter(session.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(session.response());
    }

    private String generateCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record StoredSession(OAuthTokenResponse response, Instant expiresAt) {}
}

