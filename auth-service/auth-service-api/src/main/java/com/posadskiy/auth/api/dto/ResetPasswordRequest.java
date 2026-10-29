package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record ResetPasswordRequest(
    @NotBlank @Size(max = 512) String token,
    @NotBlank @Size(min = 10, max = 72) String password) {
}
