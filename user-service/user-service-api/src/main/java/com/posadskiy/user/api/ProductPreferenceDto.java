package com.posadskiy.user.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record ProductPreferenceDto(
    @NotBlank String product,
    boolean marketingEmail,
    @Nullable @Size(max = 4000) String settings) {
}
