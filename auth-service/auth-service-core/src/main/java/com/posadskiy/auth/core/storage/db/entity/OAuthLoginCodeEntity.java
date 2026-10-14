package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import java.time.Instant;

@MappedEntity("oauth_login_code")
public class OAuthLoginCodeEntity {

    @Id
    @MappedProperty("code_hash")
    private String codeHash;

    @MappedProperty("user_id")
    private Long userId;

    @MappedProperty("external_identity_id")
    private Long externalIdentityId;

    @MappedProperty("code_challenge")
    private String codeChallenge;

    @MappedProperty("expires_at")
    private Instant expiresAt;

    @DateCreated
    private Instant createdAt;

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getExternalIdentityId() {
        return externalIdentityId;
    }

    public void setExternalIdentityId(Long externalIdentityId) {
        this.externalIdentityId = externalIdentityId;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
