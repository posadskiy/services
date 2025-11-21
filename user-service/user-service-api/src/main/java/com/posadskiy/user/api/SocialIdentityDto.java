package com.posadskiy.user.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
@Introspected
public record SocialIdentityDto(
        Long id,
        String provider,
        String providerUserId,
        String email,
        String displayName,
        String pictureUrl,
        Instant lastLoginAt,
        boolean revoked) {
}

