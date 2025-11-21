package com.posadskiy.auth.core.oauth;

import java.time.Instant;

public record ExternalProfile(
        String provider,
        String providerUserId,
        String email,
        boolean emailVerified,
        String displayName,
        String pictureUrl,
        String accessToken,
        String refreshToken,
        String idToken,
        Instant expiresAt,
        String rawClaims) {
}

