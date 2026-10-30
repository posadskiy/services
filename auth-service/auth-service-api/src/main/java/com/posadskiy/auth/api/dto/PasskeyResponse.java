package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record PasskeyResponse(String id, @Nullable String label, @Nullable String createdAt, @Nullable String lastUsedAt) {
}
