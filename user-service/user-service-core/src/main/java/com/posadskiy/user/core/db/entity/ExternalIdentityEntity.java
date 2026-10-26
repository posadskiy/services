package com.posadskiy.user.core.db.entity;

import io.micronaut.data.annotation.*;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@MappedEntity("user_social_identity")
public class ExternalIdentityEntity {

    @Id
    @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Relation(Relation.Kind.MANY_TO_ONE)
    @MappedProperty("user_id")
    private UserEntity user;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    private String provider;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @MappedProperty("provider_user_id")
    private String providerUserId;

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

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
}

