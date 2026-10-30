package com.posadskiy.user.core.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.LocalDateTime;
import java.util.Locale;

@MappedEntity("users")
public record UserEntity(
    @Id @GeneratedValue @Nullable Long id,
    String email,
    @Nullable @MappedProperty("email_normalized") String emailNormalized,
    @Nullable String passwordHash,
    @MappedProperty("email_verified") boolean emailVerified,
    @Nullable @MappedProperty("picture_url") String pictureUrl,
    @Nullable @MappedProperty("display_name") String displayName,
    @Nullable String locale,
    @Nullable @MappedProperty("time_zone") String timeZone,
    @Nullable @MappedProperty("avatar_url") String avatarUrl,
    @Nullable @MappedProperty("last_login_at") LocalDateTime lastLoginAt,
    @Nullable @MappedProperty("deactivated_at") LocalDateTime deactivatedAt,
    boolean active,
    @DateUpdated @Nullable LocalDateTime updatedAt,
    @DateCreated @Nullable LocalDateTime createdAt,
    @Version @Nullable Long version) {

    public static String normalizedEmail(@Nullable String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static UserEntity account(@Nullable Long id, @Nullable String email, @Nullable String passwordHash) {
        return new UserEntity(
            id,
            email,
            normalizedEmail(email),
            passwordHash,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null);
    }

    public UserEntity withEmailVerified(boolean verified) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            passwordHash,
            verified,
            pictureUrl,
            displayName,
            locale,
            timeZone,
            avatarUrl,
            lastLoginAt,
            deactivatedAt,
            active,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity withProfile(
        @Nullable String displayName,
        @Nullable String locale,
        @Nullable String timeZone,
        @Nullable String avatarUrl) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            passwordHash,
            emailVerified,
            pictureUrl,
            displayName,
            locale,
            timeZone,
            avatarUrl,
            lastLoginAt,
            deactivatedAt,
            active,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity erased(LocalDateTime at) {
        String tombstone = "deleted_user_" + id + "@deleted.local";
        return new UserEntity(
            id,
            tombstone,
            tombstone,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            lastLoginAt,
            at,
            false,
            updatedAt,
            createdAt,
            version);
    }
}
