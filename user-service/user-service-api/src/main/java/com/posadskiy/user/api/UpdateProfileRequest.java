package com.posadskiy.user.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record UpdateProfileRequest(
    @Nullable @Size(max = 120) String displayName,
    @Nullable @Size(max = 35) String locale,
    @Nullable @Size(max = 64) String timeZone,
    @Nullable @Size(max = 2000) String avatarUrl) {
}
