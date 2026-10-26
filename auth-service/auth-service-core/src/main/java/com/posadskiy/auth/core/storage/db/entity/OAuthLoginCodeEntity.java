package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

@MappedEntity("oauth_login_code")
public record OAuthLoginCodeEntity(
    @Id @MappedProperty("code_hash") String codeHash,
    @MappedProperty("user_id") Long userId,
    @Nullable @MappedProperty("external_identity_id") Long externalIdentityId,
    @Nullable @MappedProperty("code_challenge") String codeChallenge,
    @MappedProperty("expires_at") Instant expiresAt,
    @DateCreated @Nullable Instant createdAt) {
}
