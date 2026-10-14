package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

/** @deprecated Use {@link RegisterRequest} and {@link UserResponse}. */
@Deprecated
@Serdeable
@Introspected
public record UserDto(
        @Nullable String id,
        @NotBlank String username,
        @NotBlank String email,
        @Nullable String password) {}
