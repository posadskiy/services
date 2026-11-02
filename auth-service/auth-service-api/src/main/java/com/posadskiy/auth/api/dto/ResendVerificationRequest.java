package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resend the email-verification link.
 *
 * <p>Anonymous clients send {@code email} (and captcha when Turnstile is configured). Signed-in
 * clients may omit {@code email}; the current account is used instead.
 */
@Serdeable
@Introspected
public record ResendVerificationRequest(
    @Nullable @Email @Size(max = 255) String email,
    @NotBlank @Size(max = 255) String linkOrigin,
    @Nullable @Size(max = 2048) String captchaToken) {

    public ResendVerificationRequest(String email, String linkOrigin) {
        this(email, linkOrigin, null);
    }
}
