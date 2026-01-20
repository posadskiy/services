package com.posadskiy.user.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

@Serdeable
@Introspected
public record UpdateUsernameRequest(
        @NonNull @NotBlank String username) {
}
