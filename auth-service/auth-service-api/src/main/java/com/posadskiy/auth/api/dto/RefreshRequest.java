package com.posadskiy.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

@Serdeable
@Introspected
public record RefreshRequest(
    @NotBlank @JsonProperty("grant_type") String grantType,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("client_id") String clientId,
    @JsonProperty("client_secret") String clientSecret,
    String username,
    String password) {
}
