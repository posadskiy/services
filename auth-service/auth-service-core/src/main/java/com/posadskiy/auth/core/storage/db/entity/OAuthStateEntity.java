package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

@MappedEntity("oauth_state")
public record OAuthStateEntity(
    @Id @MappedProperty("state_value") String stateValue,
    String provider,
    @MappedProperty("redirect_uri") String redirectUri,
    @MappedProperty("code_verifier") String codeVerifier,
    String nonce,
    @MappedProperty("browser_binding_hash") String browserBindingHash,
    @Nullable @MappedProperty("code_challenge") String codeChallenge,
    @Nullable @MappedProperty("frontend_redirect_url") String frontendRedirectUrl,
    @MappedProperty("expires_at") Instant expiresAt,
    @DateCreated @Nullable Instant createdAt) {
}
