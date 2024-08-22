package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Serdeable
@Introspected
public record UserDto(
    @Nullable String id,
    @NonNull @NotBlank String username,
    @NonNull @NotNull String email,
    @NonNull @NotNull String password
) {
}
