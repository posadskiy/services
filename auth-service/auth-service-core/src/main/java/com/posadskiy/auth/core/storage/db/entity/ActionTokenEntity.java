package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.Instant;

@MappedEntity("action_token")
public record ActionTokenEntity(
    @Id @GeneratedValue @Nullable Long id,
    @MappedProperty("user_id") Long userId,
    String purpose,
    @MappedProperty("token_hash") String tokenHash,
    @MappedProperty("expires_at") Instant expiresAt,
    @Nullable @MappedProperty("consumed_at") Instant consumedAt,
    @DateCreated @Nullable Instant createdAt) {
}
