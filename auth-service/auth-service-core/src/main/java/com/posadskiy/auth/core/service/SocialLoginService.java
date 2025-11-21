package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.oauth.TokenEncryptionService;
import com.posadskiy.auth.core.storage.db.ExternalIdentityRepository;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.ExternalIdentityEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Singleton
public class SocialLoginService {

    private final UsersRepository usersRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public SocialLoginService(
            UsersRepository usersRepository,
            ExternalIdentityRepository externalIdentityRepository,
            TokenEncryptionService tokenEncryptionService) {
        this.usersRepository = usersRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    public SocialLoginResult handle(ExternalProfile profile) {
        if (profile.providerUserId() == null) {
            throw new IllegalArgumentException("Provider user id missing");
        }

        Optional<ExternalIdentityEntity> existingIdentity =
                externalIdentityRepository.findByProviderAndProviderUserId(
                        profile.provider(), profile.providerUserId());

        ExternalIdentityEntity identity =
                existingIdentity.orElseGet(() -> createIdentitySkeleton(profile));

        UserEntity user = existingIdentity
                .map(ei -> usersRepository.findById(ei.getUserId()).orElseThrow())
                .orElseGet(() -> resolveUserForProfile(profile));

        identity.setUserId(user.getId());
        identity.setEmail(profile.email());
        identity.setDisplayName(profile.displayName());
        identity.setPictureUrl(profile.pictureUrl());
        identity.setLastLoginAt(LocalDateTime.now());
        identity.setAccessTokenEncrypted(tokenEncryptionService.encrypt(profile.accessToken()));
        identity.setRefreshTokenEncrypted(tokenEncryptionService.encrypt(profile.refreshToken()));
        identity.setRawClaims(profile.rawClaims());
        identity.setRevoked(Boolean.FALSE);
        identity.setScopes(null);
        identity.setExpiresAt(profile.expiresAt() != null
                ? LocalDateTime.ofInstant(profile.expiresAt(), ZoneOffset.UTC)
                : null);

        if (existingIdentity.isPresent()) {
            externalIdentityRepository.update(identity);
        } else {
            externalIdentityRepository.save(identity);
        }

        user.setLastLoginAt(LocalDateTime.now());
        if (profile.emailVerified()) {
            user.setEmailVerified(Boolean.TRUE);
        }
        if (StringUtils.isNotEmpty(profile.pictureUrl())) {
            user.setPictureUrl(profile.pictureUrl());
        }
        usersRepository.update(user);

        return new SocialLoginResult(user, identity);
    }

    private ExternalIdentityEntity createIdentitySkeleton(ExternalProfile profile) {
        ExternalIdentityEntity entity = new ExternalIdentityEntity();
        entity.setProvider(profile.provider());
        entity.setProviderUserId(profile.providerUserId());
        return entity;
    }

    private UserEntity resolveUserForProfile(ExternalProfile profile) {
        return usersRepository
                .findByEmail(profile.email())
                .map(existing -> updateExistingUser(existing, profile))
                .orElseGet(() -> createUserFromProfile(profile));
    }

    private UserEntity updateExistingUser(UserEntity user, ExternalProfile profile) {
        if (profile.emailVerified()) {
            user.setEmailVerified(Boolean.TRUE);
        }
        if (StringUtils.isNotEmpty(profile.pictureUrl())) {
            user.setPictureUrl(profile.pictureUrl());
        }
        return usersRepository.update(user);
    }

    private UserEntity createUserFromProfile(ExternalProfile profile) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(buildUsername(profile));
        userEntity.setEmail(profile.email());
        userEntity.setEmailVerified(profile.emailVerified());
        userEntity.setPasswordHash(null);
        userEntity.setPictureUrl(profile.pictureUrl());
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());
        return usersRepository.save(userEntity);
    }

    private String buildUsername(ExternalProfile profile) {
        if (StringUtils.isNotEmpty(profile.displayName())) {
            return profile.displayName().replaceAll("\\s+", ".").toLowerCase();
        }
        if (StringUtils.isNotEmpty(profile.email())) {
            return profile.email().split("@")[0];
        }
        return profile.provider() + "_" + profile.providerUserId();
    }

    public record SocialLoginResult(UserEntity user, ExternalIdentityEntity identity) {}
}

