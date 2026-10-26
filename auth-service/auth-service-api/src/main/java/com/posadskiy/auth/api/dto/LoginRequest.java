package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record LoginRequest(
    @NotBlank @Size(max = 255) String username,
    @NotBlank @Size(max = 72) String password) {
}
