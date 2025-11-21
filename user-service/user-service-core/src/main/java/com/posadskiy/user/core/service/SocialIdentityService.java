package com.posadskiy.user.core.service;

import com.posadskiy.user.api.LinkSocialIdentityRequest;
import com.posadskiy.user.api.SocialIdentityDto;
import com.posadskiy.user.core.db.ExternalIdentityRepository;
import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.ExternalIdentityEntity;
import com.posadskiy.user.core.db.entity.UserEntity;
import jakarta.inject.Singleton;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class SocialIdentityService {

    private final ExternalIdentityRepository externalIdentityRepository;
    private final UsersRepository usersRepository;

    public SocialIdentityService(
            ExternalIdentityRepository externalIdentityRepository, UsersRepository usersRepository) {
        this.externalIdentityRepository = externalIdentityRepository;
        this.usersRepository = usersRepository;
    }

    public List<SocialIdentityDto> listIdentities(Long userId) {
        return externalIdentityRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public SocialIdentityDto linkIdentity(Long userId, LinkSocialIdentityRequest request) {
        UserEntity user = usersRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ExternalIdentityEntity entity =
                externalIdentityRepository
                        .findByProviderAndProviderUserId(request.provider(), request.providerUserId())
                        .orElseGet(ExternalIdentityEntity::new);

        entity.setUser(user);
        entity.setProvider(request.provider());
        entity.setProviderUserId(request.providerUserId());
        entity.setEmail(request.email());
        entity.setDisplayName(request.displayName());
        entity.setPictureUrl(request.pictureUrl());
        entity.setRevoked(Boolean.FALSE);
        entity.setLastLoginAt(LocalDateTime.now());
        ExternalIdentityEntity saved = externalIdentityRepository.save(entity);

        if (request.emailVerified()) {
            user.setEmailVerified(Boolean.TRUE);
            usersRepository.update(user);
        }

        return mapToDto(saved);
    }

    public void unlinkIdentity(Long userId, String provider) {
        List<ExternalIdentityEntity> identities = externalIdentityRepository.findByUserId(userId);
        identities.stream()
                .filter(identity -> identity.getProvider().equalsIgnoreCase(provider))
                .forEach(
                        identity -> {
                            identity.setRevoked(Boolean.TRUE);
                            externalIdentityRepository.update(identity);
                        });
    }

    private SocialIdentityDto mapToDto(ExternalIdentityEntity entity) {
        return new SocialIdentityDto(
                entity.getId(),
                entity.getProvider(),
                entity.getProviderUserId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPictureUrl(),
                entity.getLastLoginAt() != null
                        ? entity.getLastLoginAt().atZone(ZoneOffset.UTC).toInstant()
                        : null,
                Boolean.TRUE.equals(entity.getRevoked()));
    }
}

