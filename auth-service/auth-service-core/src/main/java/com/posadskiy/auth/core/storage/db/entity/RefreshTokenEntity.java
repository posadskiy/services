package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.Instant;
import java.util.UUID;

@MappedEntity("refresh_token")
public record RefreshTokenEntity(
    @Id @GeneratedValue @Nullable Long id,
    @MappedProperty("user_id") Long userId,
    @MappedProperty("token_hash") String tokenHash,
    @MappedProperty("family_id") UUID familyId,
    @Nullable @MappedProperty("replaced_by_id") Long replacedById,
    @Nullable String provider,
    @Nullable @MappedProperty("external_identity_id") Long externalIdentityId,
    boolean revoked,
    @MappedProperty("expires_at") Instant expiresAt,
    @Nullable @MappedProperty("last_used_at") Instant lastUsedAt,
    @DateCreated @Nullable Instant createdAt) {

    public RefreshTokenEntity consumed(Instant usedAt, Long replacementId) {
        return new RefreshTokenEntity(
            id,
            userId,
            tokenHash,
            familyId,
            replacementId,
            provider,
            externalIdentityId,
            true,
            expiresAt,
            usedAt,
            createdAt);
    }
}
