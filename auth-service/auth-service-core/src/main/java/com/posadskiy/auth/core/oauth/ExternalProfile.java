package com.posadskiy.auth.core.oauth;

public record ExternalProfile(
    String provider,
    String providerUserId,
    String email,
    boolean emailVerified,
    String displayName,
    String pictureUrl) {
}
