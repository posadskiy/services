package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@MappedEntity("users")
public record UserEntity(
    @Id @GeneratedValue @Nullable Long id,
    String email,
    @MappedProperty("email_normalized") String emailNormalized,
    @Nullable @MappedProperty("password_hash") String passwordHash,
    @MappedProperty("email_verified") boolean emailVerified,
    @Nullable @MappedProperty("picture_url") String pictureUrl,
    String roles,
    @Nullable @MappedProperty("last_login_at") Instant lastLoginAt,
    @Nullable @MappedProperty("password_changed_at") Instant passwordChangedAt,
    boolean active,
    @Nullable @MappedProperty("deactivated_at") Instant deactivatedAt,
    @DateUpdated @Nullable Instant updatedAt,
    @DateCreated @Nullable Instant createdAt,
    @Version @Nullable Long version) {

    public UserEntity {
        if (roles == null || roles.isBlank()) {
            roles = "ROLE_USER";
        }
    }

    public static UserEntity newAccount(
        String email,
        String emailNormalized,
        String passwordHash,
        boolean emailVerified,
        String pictureUrl,
        Instant passwordChangedAt) {
        return new UserEntity(
            null,
            email,
            emailNormalized,
            passwordHash,
            emailVerified,
            pictureUrl,
            "ROLE_USER",
            null,
            passwordChangedAt,
            true,
            null,
            null,
            null,
            null);
    }

    public List<String> roleList() {
        return Arrays.stream(roles.split(","))
            .map(String::trim)
            .filter(role -> !role.isEmpty())
            .toList();
    }

    public UserEntity loggedInAt(Instant at) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            passwordHash,
            emailVerified,
            pictureUrl,
            roles,
            at,
            passwordChangedAt,
            active,
            deactivatedAt,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity withProfile(Instant loggedInAt, boolean verified, @Nullable String picture) {
        String nextPicture = picture == null || picture.isBlank() ? pictureUrl : picture;
        return new UserEntity(
            id,
            email,
            emailNormalized,
            passwordHash,
            emailVerified || verified,
            nextPicture,
            roles,
            loggedInAt,
            passwordChangedAt,
            active,
            deactivatedAt,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity withEmailVerified(boolean verified) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            passwordHash,
            verified,
            pictureUrl,
            roles,
            lastLoginAt,
            passwordChangedAt,
            active,
            deactivatedAt,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity claimedByVerifiedProvider(Instant at) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            null,
            true,
            pictureUrl,
            roles,
            lastLoginAt,
            at,
            active,
            deactivatedAt,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity withPassword(String hash, Instant changedAt) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            hash,
            emailVerified,
            pictureUrl,
            roles,
            lastLoginAt,
            changedAt,
            active,
            deactivatedAt,
            updatedAt,
            createdAt,
            version);
    }

    public UserEntity deactivatedAt(Instant at) {
        return new UserEntity(
            id,
            email,
            emailNormalized,
            passwordHash,
            emailVerified,
            pictureUrl,
            roles,
            lastLoginAt,
            passwordChangedAt,
            false,
            at,
            updatedAt,
            createdAt,
            version);
    }
}
