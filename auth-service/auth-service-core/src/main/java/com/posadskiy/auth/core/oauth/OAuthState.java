package com.posadskiy.auth.core.oauth;

import java.time.Instant;

public record OAuthState(
        String value,
        String provider,
        String redirectUri,
        String codeVerifier,
        String nonce,
        Instant expiresAt) {
}

