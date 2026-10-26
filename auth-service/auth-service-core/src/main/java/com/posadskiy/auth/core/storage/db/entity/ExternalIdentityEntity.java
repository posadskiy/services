package com.posadskiy.auth.core.storage.db.entity;

import com.posadskiy.auth.core.oauth.ExternalProfile;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.Instant;

@MappedEntity("user_social_identity")
public record ExternalIdentityEntity(
    @Id @GeneratedValue @Nullable Long id,
    @MappedProperty("user_id") Long userId,
    String provider,
    @MappedProperty("provider_user_id") String providerUserId,
    @Nullable String email,
    @Nullable @MappedProperty("display_name") String displayName,
    @Nullable @MappedProperty("picture_url") String pictureUrl,
    @Nullable @MappedProperty("last_login_at") Instant lastLoginAt,
    @DateCreated @Nullable Instant createdAt,
    @DateUpdated @Nullable Instant updatedAt,
    boolean revoked) {

    public static ExternalIdentityEntity linked(Long userId, ExternalProfile profile, Instant at) {
        return new ExternalIdentityEntity(
            null,
            userId,
            profile.provider(),
            profile.providerUserId(),
            profile.email(),
            profile.displayName(),
            profile.pictureUrl(),
            at,
            null,
            null,
            false);
    }

    public ExternalIdentityEntity refreshed(ExternalProfile profile, Instant at) {
        return new ExternalIdentityEntity(
            id,
            userId,
            provider,
            providerUserId,
            profile.email(),
            profile.displayName(),
            profile.pictureUrl(),
            at,
            createdAt,
            updatedAt,
            false);
    }
}
