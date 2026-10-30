package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record SessionResponse(
    String id,
    @Nullable String provider,
    @Nullable String userAgent,
    @Nullable String clientIp,
    @Nullable String createdAt) {
}
