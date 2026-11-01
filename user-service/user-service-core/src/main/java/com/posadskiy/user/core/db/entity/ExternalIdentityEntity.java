package com.posadskiy.user.core.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.LocalDateTime;

@MappedEntity("user_social_identity")
public record ExternalIdentityEntity(
    @Id @GeneratedValue @Nullable Long id,
    @MappedProperty("user_id") Long userId,
    String provider,
    @MappedProperty("provider_user_id") String providerUserId,
    @Nullable String email,
    @Nullable @MappedProperty("display_name") String displayName,
    @Nullable @MappedProperty("picture_url") String pictureUrl,
    @Nullable @MappedProperty("last_login_at") LocalDateTime lastLoginAt,
    @DateCreated @Nullable LocalDateTime createdAt,
    @DateUpdated @Nullable LocalDateTime updatedAt,
    boolean revoked) {

    public static ExternalIdentityEntity linked(
        Long userId,
        String provider,
        String providerUserId,
        @Nullable String email,
        @Nullable String displayName,
        @Nullable String pictureUrl,
        LocalDateTime at) {
        return new ExternalIdentityEntity(
            null,
            userId,
            provider,
            providerUserId,
            email,
            displayName,
            pictureUrl,
            at,
            null,
            null,
            false);
    }

    public ExternalIdentityEntity refreshed(
        Long userId,
        String provider,
        String providerUserId,
        @Nullable String email,
        @Nullable String displayName,
        @Nullable String pictureUrl,
        LocalDateTime at) {
        return new ExternalIdentityEntity(
            id,
            userId,
            provider,
            providerUserId,
            email,
            displayName,
            pictureUrl,
            at,
            createdAt,
            updatedAt,
            false);
    }

    public ExternalIdentityEntity withRevoked() {
        return new ExternalIdentityEntity(
            id,
            userId,
            provider,
            providerUserId,
            email,
            displayName,
            pictureUrl,
            lastLoginAt,
            createdAt,
            updatedAt,
            true);
    }
}
