package com.posadskiy.user.core.service;

import com.posadskiy.user.api.LinkSocialIdentityRequest;
import com.posadskiy.user.api.SocialIdentityDto;
import com.posadskiy.user.api.UserIds;
import com.posadskiy.user.core.db.ExternalIdentityRepository;
import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.ExternalIdentityEntity;
import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.exception.AuthException;
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

        LocalDateTime now = LocalDateTime.now();
        ExternalIdentityEntity entity = externalIdentityRepository
            .findByProviderAndProviderUserId(request.provider(), request.providerUserId())
            .map(existing -> existing.refreshed(
                user.id(),
                request.provider(),
                request.providerUserId(),
                request.email(),
                request.displayName(),
                request.pictureUrl(),
                now))
            .orElseGet(() -> ExternalIdentityEntity.linked(
                user.id(),
                request.provider(),
                request.providerUserId(),
                request.email(),
                request.displayName(),
                request.pictureUrl(),
                now));
        ExternalIdentityEntity saved = externalIdentityRepository.save(entity);

        if (request.emailVerified()) {
            usersRepository.update(user.withEmailVerified(true));
        }

        return mapToDto(saved);
    }

    public void unlinkIdentity(Long userId, String provider) {
        UserEntity user = usersRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<ExternalIdentityEntity> identities = externalIdentityRepository.findByUserId(userId);
        long others = identities.stream()
            .filter(identity -> !identity.revoked())
            .filter(identity -> !identity.provider().equalsIgnoreCase(provider))
            .count();
        if (user.passwordHash() == null && others == 0) {
            throw new AuthException("Add a password or another sign-in method before unlinking");
        }
        identities.stream()
            .filter(identity -> identity.provider().equalsIgnoreCase(provider))
            .forEach(identity -> externalIdentityRepository.save(identity.withRevoked()));
    }

    private SocialIdentityDto mapToDto(ExternalIdentityEntity entity) {
        return new SocialIdentityDto(
            entity.id() == null ? null : UserIds.of(entity.id()),
            entity.provider(),
            entity.providerUserId(),
            entity.email(),
            entity.displayName(),
            entity.pictureUrl(),
            entity.lastLoginAt() != null
                ? entity.lastLoginAt().atZone(ZoneOffset.UTC).toInstant()
                : null,
            entity.revoked());
    }
}
