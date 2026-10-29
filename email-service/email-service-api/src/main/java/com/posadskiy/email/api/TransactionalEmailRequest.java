package com.posadskiy.email.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Serdeable
@Introspected
public record TransactionalEmailRequest(
        @NotBlank @Size(max = 320) String from,
        @NotBlank @Email @Size(max = 255) String to,
        @NotBlank @Size(max = 64) String template,
        @NotNull Map<@NotBlank @Size(max = 50) String, @NotNull @Size(max = 2000) String> variables,
        @NotBlank @Size(max = 256) String idempotencyKey,
        @NotBlank @Size(max = 64) String category) {}
