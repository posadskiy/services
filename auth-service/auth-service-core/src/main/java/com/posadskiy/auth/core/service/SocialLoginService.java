package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.storage.db.ExternalIdentityRepository;
import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.ExternalIdentityEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Optional;

@Singleton
public class SocialLoginService {

    private final UsersRepository usersRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    public SocialLoginService(
        UsersRepository usersRepository,
        ExternalIdentityRepository externalIdentityRepository,
        RefreshTokenRepository refreshTokenRepository,
        AuditService auditService) {
        this.usersRepository = usersRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SocialLoginResult handle(ExternalProfile profile) {
        if (profile.providerUserId() == null) {
            throw new OAuthException("Provider user id missing");
        }
        Optional<ExternalIdentityEntity> existingIdentity = externalIdentityRepository.findByProviderAndProviderUserId(
            profile.provider(), profile.providerUserId());
        if (existingIdentity.isPresent()) {
            ExternalIdentityEntity identity = existingIdentity.get();
            if (identity.revoked()) {
                throw new OAuthException("This social identity has been revoked", 403);
            }
            UserEntity user = usersRepository
                .findById(identity.userId())
                .orElseThrow(() -> new OAuthException("Linked user is missing"));
            if (!user.active()) {
                throw new OAuthException("Account is disabled", 403);
            }
            identity = externalIdentityRepository.update(identity.refreshed(profile, Instant.now()));
            user = touchUser(user, profile);
            return new SocialLoginResult(user, identity);
        }

        UserEntity user = resolveUserForNewIdentity(profile);
        ExternalIdentityEntity identity =
            externalIdentityRepository.save(ExternalIdentityEntity.linked(user.id(), profile, Instant.now()));
        user = touchUser(user, profile);
        auditService.record(user.id(), "SOCIAL_LINKED", profile.provider());
        return new SocialLoginResult(user, identity);
    }

    private UserEntity resolveUserForNewIdentity(ExternalProfile profile) {
        String normalized = EmailNormalizer.normalize(profile.email());
        if (normalized == null) {
            return createUserFromProfile(profile, uniqueFallbackEmail(profile));
        }
        Optional<UserEntity> existing = usersRepository.findByEmailNormalized(normalized);
        if (existing.isEmpty()) {
            return createUserFromProfile(profile, profile.email());
        }
        if (!profile.emailVerified()) {
            throw new OAuthException(
                "This email already has an account. Sign in and link this provider after verifying ownership.");
        }
        UserEntity user = existing.get();
        if (!user.active()) {
            throw new OAuthException("Account is disabled", 403);
        }
        if (!user.emailVerified()) {
            user = usersRepository.update(user.claimedByVerifiedProvider(Instant.now()));
            refreshTokenRepository.revokeAllForUser(user.id());
            auditService.record(user.id(), "UNVERIFIED_ACCOUNT_CLAIMED", profile.provider());
        }
        return user;
    }

    private UserEntity createUserFromProfile(ExternalProfile profile, String email) {
        String normalized = EmailNormalizer.normalize(email);
        UserEntity saved = usersRepository.save(UserEntity.newAccount(
            email,
            normalized,
            null,
            profile.emailVerified(),
            profile.pictureUrl(),
            null));
        auditService.record(saved.id(), "USER_REGISTERED", profile.provider());
        return saved;
    }

    private UserEntity touchUser(UserEntity user, ExternalProfile profile) {
        return usersRepository.update(user.withProfile(Instant.now(), profile.emailVerified(), profile.pictureUrl()));
    }

    private String uniqueFallbackEmail(ExternalProfile profile) {
        return profile.provider() + "+" + profile.providerUserId() + "@users.noreply.local";
    }

    public record SocialLoginResult(UserEntity user, ExternalIdentityEntity identity) {
    }
}
