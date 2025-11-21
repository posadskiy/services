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

    @Relation(Relation.Kind.MANY_TO_ONE)
    @MappedProperty("user_id")
    private UserEntity user;

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
}

