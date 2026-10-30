package com.posadskiy.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Serdeable
@Introspected
public record RefreshRequest(
    @NotBlank @JsonProperty("grant_type") String grantType,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_secret") String clientSecret,
    String email,
    String password,
    @Nullable @Size(max = 2048) @JsonProperty("captcha_token") String captchaToken) {

    public RefreshRequest(
        String grantType, String refreshToken, String clientId, String clientSecret, String email, String password) {
        this(grantType, refreshToken, clientId, clientSecret, email, password, null);
    }
}
