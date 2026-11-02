package com.posadskiy.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** A native app's PKCE S256 challenge: 43 base64url characters. */
@Serdeable
@Introspected
public record AppCodeRequest(
    @JsonProperty("code_challenge") @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String codeChallenge) {
}
