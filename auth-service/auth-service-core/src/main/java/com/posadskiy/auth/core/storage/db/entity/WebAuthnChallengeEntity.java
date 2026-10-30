package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

@MappedEntity("webauthn_challenge")
public record WebAuthnChallengeEntity(
    @Id @GeneratedValue @Nullable Long id,
    @Nullable @MappedProperty("user_id") Long userId,
    String purpose,
    @MappedProperty("request_json") String requestJson,
    @MappedProperty("expires_at") Instant expiresAt,
    @DateCreated @Nullable Instant createdAt) {
}
