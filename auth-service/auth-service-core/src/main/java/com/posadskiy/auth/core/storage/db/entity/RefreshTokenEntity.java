package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import java.time.Instant;
import java.util.UUID;

@MappedEntity("refresh_token")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    @MappedProperty("token_hash")
    private String tokenHash;

    @MappedProperty("family_id")
    private UUID familyId;

    @MappedProperty("replaced_by_id")
    private Long replacedById;

    private String provider;

    @MappedProperty("external_identity_id")
    private Long externalIdentityId;

    private Boolean revoked = Boolean.FALSE;

    @MappedProperty("expires_at")
    private Instant expiresAt;

    @MappedProperty("last_used_at")
    private Instant lastUsedAt;

    @DateCreated
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public Long getReplacedById() {
        return replacedById;
    }

    public void setReplacedById(Long replacedById) {
        this.replacedById = replacedById;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Long getExternalIdentityId() {
        return externalIdentityId;
    }

    public void setExternalIdentityId(Long externalIdentityId) {
        this.externalIdentityId = externalIdentityId;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
