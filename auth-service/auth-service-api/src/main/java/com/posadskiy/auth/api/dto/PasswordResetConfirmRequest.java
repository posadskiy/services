package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 10, max = 72) String password) {}
