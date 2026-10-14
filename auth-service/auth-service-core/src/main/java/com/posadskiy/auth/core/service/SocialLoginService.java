package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.storage.db.ExternalIdentityRepository;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.ExternalIdentityEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.core.util.StringUtils;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;

@Singleton
public class SocialLoginService {

    private final UsersRepository usersRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final AuditService auditService;

    public SocialLoginService(
            UsersRepository usersRepository,
            ExternalIdentityRepository externalIdentityRepository,
            AuditService auditService) {
        this.usersRepository = usersRepository;
        this.externalIdentityRepository = externalIdentityRepository;
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
            if (Boolean.TRUE.equals(identity.getRevoked())) {
                throw new OAuthException("This social identity has been revoked", 403);
            }
            UserEntity user = usersRepository
                    .findById(identity.getUserId())
                    .orElseThrow(() -> new OAuthException("Linked user is missing"));
            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new OAuthException("Account is disabled", 403);
            }
            updateIdentity(identity, profile);
            touchUser(user, profile);
            return new SocialLoginResult(user, identity);
        }

        UserEntity user = resolveUserForNewIdentity(profile);
        ExternalIdentityEntity identity = new ExternalIdentityEntity();
        identity.setProvider(profile.provider());
        identity.setProviderUserId(profile.providerUserId());
        identity.setUserId(user.getId());
        identity.setRevoked(Boolean.FALSE);
        updateIdentity(identity, profile);
        identity = externalIdentityRepository.save(identity);
        touchUser(user, profile);
        auditService.record(user.getId(), "SOCIAL_LINKED", profile.provider());
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
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new OAuthException("Account is disabled", 403);
        }
        return user;
    }

    private UserEntity createUserFromProfile(ExternalProfile profile, String email) {
        UserEntity user = new UserEntity();
        String normalized = EmailNormalizer.normalize(email);
        user.setEmail(email);
        user.setEmailNormalized(normalized);
        user.setUsername(buildUsername(profile, normalized));
        user.setEmailVerified(profile.emailVerified());
        user.setPasswordHash(null);
        user.setPictureUrl(profile.pictureUrl());
        user.setRoles("ROLE_USER");
        user.setActive(Boolean.TRUE);
        UserEntity saved = usersRepository.save(user);
        auditService.record(saved.getId(), "USER_REGISTERED", profile.provider());
        return saved;
    }

    private void updateIdentity(ExternalIdentityEntity identity, ExternalProfile profile) {
        identity.setEmail(profile.email());
        identity.setDisplayName(profile.displayName());
        identity.setPictureUrl(profile.pictureUrl());
        identity.setLastLoginAt(Instant.now());
        identity.setRevoked(Boolean.FALSE);
        if (identity.getId() != null) {
            externalIdentityRepository.update(identity);
        }
    }

    private void touchUser(UserEntity user, ExternalProfile profile) {
        user.setLastLoginAt(Instant.now());
        if (profile.emailVerified()) {
            user.setEmailVerified(Boolean.TRUE);
        }
        if (StringUtils.isNotEmpty(profile.pictureUrl())) {
            user.setPictureUrl(profile.pictureUrl());
        }
        usersRepository.update(user);
    }

    private String buildUsername(ExternalProfile profile, String email) {
        String base;
        if (StringUtils.isNotEmpty(profile.displayName())) {
            base = profile.displayName().replaceAll("\\s+", ".").toLowerCase();
        } else if (email != null && email.contains("@")) {
            base = email.substring(0, email.indexOf('@'));
        } else {
            base = profile.provider() + "_" + profile.providerUserId();
        }
        String candidate = base;
        int attempts = 0;
        while (usersRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + SecureTokens.randomUrl(3);
            if (++attempts > 8) {
                candidate = profile.provider() + "_" + SecureTokens.randomUrl(6);
                break;
            }
        }
        return candidate;
    }

    private String uniqueFallbackEmail(ExternalProfile profile) {
        return profile.provider() + "+" + profile.providerUserId() + "@users.noreply.local";
    }

    public record SocialLoginResult(UserEntity user, ExternalIdentityEntity identity) {}
}
