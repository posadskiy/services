package com.posadskiy.auth.core.oauth;

import jakarta.inject.Singleton;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class OAuthStateStore {

    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OAuthState> states = new ConcurrentHashMap<>();

    public OAuthStateStore(SocialOAuthConfigurationProperties configuration) {
        this.ttl = configuration.getStateTtl();
    }

    public OAuthState create(String provider, String redirectUri, String codeVerifier, String nonce) {
        String stateValue = generateStateValue();
        OAuthState state =
                new OAuthState(
                        stateValue,
                        provider,
                        redirectUri,
                        codeVerifier,
                        nonce,
                        Instant.now().plus(ttl));
        states.put(stateValue, state);
        return state;
    }

    public Optional<OAuthState> consume(String value) {
        if (value == null) {
            return Optional.empty();
        }
        OAuthState state = states.remove(value);
        if (state == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(state.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    private String generateStateValue() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

