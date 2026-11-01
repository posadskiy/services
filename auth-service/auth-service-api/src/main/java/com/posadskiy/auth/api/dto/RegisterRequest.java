package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record RegisterRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 10, max = 72) String password,
    @NotBlank @Size(max = 255) String linkOrigin,
    @Nullable @Size(max = 2048) String captchaToken,
    @Nullable @Size(max = 35) String locale,
    @Nullable @Size(max = 64) String timeZone) {

    public RegisterRequest(String email, String password, String linkOrigin) {
        this(email, password, linkOrigin, null, null, null);
    }

    public RegisterRequest(String email, String password, String linkOrigin, String captchaToken) {
        this(email, password, linkOrigin, captchaToken, null, null);
    }
}
