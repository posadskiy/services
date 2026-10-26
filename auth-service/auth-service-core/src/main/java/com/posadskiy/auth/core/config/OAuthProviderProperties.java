package com.posadskiy.auth.core.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Nullable;

import java.util.List;

@EachProperty("auth.oauth.providers")
public record OAuthProviderProperties(
    @Parameter String name,
    boolean enabled,
    @Nullable String clientId,
    @Nullable String clientSecret,
    @Nullable String authorizationUri,
    @Nullable String tokenUri,
    @Nullable String jwksUri,
    @Nullable String issuer,
    @Nullable String redirectUri,
    @Nullable List<String> scopes,
    @Nullable String appleTeamId,
    @Nullable String appleKeyId,
    @Nullable String applePrivateKeyPem) {

    public OAuthProviderProperties {
        scopes = scopes == null ? List.of() : List.copyOf(scopes);
    }
}
