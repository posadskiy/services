package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@MappedEntity("user_social_identity")
public class ExternalIdentityEntity {

    @Id
    @GeneratedValue
    @NonNull
    private Long id;

    @MappedProperty("user_id")
    @NonNull
    @NotNull
    private Long userId;

    private String provider;

    @MappedProperty("provider_user_id")
    private String providerUserId;

    private String email;

    @MappedProperty("display_name")
    private String displayName;

    @MappedProperty("picture_url")
    private String pictureUrl;

    @MappedProperty("access_token_encrypted")
    private String accessTokenEncrypted;

    @MappedProperty("refresh_token_encrypted")
    private String refreshTokenEncrypted;

    @MappedProperty("expires_at")
    private LocalDateTime expiresAt;

    private String scopes;

    @MappedProperty("raw_claims")
    private String rawClaims;

    @MappedProperty("last_login_at")
    private LocalDateTime lastLoginAt;

    @DateCreated
    private LocalDateTime createdAt;

    @DateUpdated
    private LocalDateTime updatedAt;

    private Boolean revoked = Boolean.FALSE;

    public @NonNull Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public @NonNull Long getUserId() {
        return userId;
    }

    public void setUserId(@NonNull Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    public void setAccessTokenEncrypted(String accessTokenEncrypted) {
        this.accessTokenEncrypted = accessTokenEncrypted;
    }

    public String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) {
        this.refreshTokenEncrypted = refreshTokenEncrypted;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getRawClaims() {
        return rawClaims;
    }

    public void setRawClaims(String rawClaims) {
        this.rawClaims = rawClaims;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

