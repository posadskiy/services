package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.AccountCleanupRepository;
import com.posadskiy.user.core.db.ExternalIdentityRepository;
import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.exception.AuthException;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class UserService {
    private final UserEntityMapper userEntityMapper;
    private final UsersRepository userRepository;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final AccountCleanupRepository accountCleanupRepository;

    public UserService(
        UserEntityMapper userEntityMapper,
        UsersRepository userRepository,
        ExternalIdentityRepository externalIdentityRepository,
        AccountCleanupRepository accountCleanupRepository) {
        this.userEntityMapper = userEntityMapper;
        this.userRepository = userRepository;
        this.externalIdentityRepository = externalIdentityRepository;
        this.accountCleanupRepository = accountCleanupRepository;
    }

    public User getUserById(Long id) {
        var entity = userRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("User with id " + id + " not found"));

        // Check if user is active
        if (Boolean.FALSE.equals(entity.getActive())) {
            throw new AuthException("User account has been deactivated");
        }

        User baseUser = userEntityMapper.mapFromEntity(entity);
        List<String> providers = externalIdentityRepository.findByUserId(id).stream()
            .map(identity -> identity.getProvider().toLowerCase())
            .distinct()
            .collect(Collectors.toList());
        return new User(
            baseUser.id(),
            baseUser.email(),
            baseUser.password(),
            baseUser.emailVerified(),
            baseUser.pictureUrl(),
            baseUser.createdVia(),
            providers,
            baseUser.active(),
            baseUser.deactivatedAt(),
            baseUser.displayName(),
            baseUser.locale(),
            baseUser.timeZone(),
            baseUser.avatarUrl());
    }

    /**
     * Permanently delete or anonymize user data for GDPR compliance (Right to Erasure).
     * This is the primary method for user deletion and is GDPR-compliant.
     *
     * @param id User ID to delete
     */
    public void deleteUser(Long id) {
        UserEntity existingEntity = userRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("User with id " + id + " not found"));

        String normalized = existingEntity.getEmailNormalized();
        if (normalized != null && !normalized.endsWith("@deleted.local")) {
            accountCleanupRepository.suppress(normalized);
        }
        accountCleanupRepository.revokeTokens(id);
        accountCleanupRepository.deletePasskeys(id);
        accountCleanupRepository.deletePreferences(id);

        existingEntity.setEmail("deleted_user_" + id + "@deleted.local");
        existingEntity.setPasswordHash(null);
        existingEntity.setPictureUrl(null);
        existingEntity.setDisplayName(null);
        existingEntity.setLocale(null);
        existingEntity.setTimeZone(null);
        existingEntity.setAvatarUrl(null);
        existingEntity.setEmailVerified(Boolean.FALSE);
        existingEntity.setActive(Boolean.FALSE);
        existingEntity.setDeactivatedAt(LocalDateTime.now());
        userRepository.update(existingEntity);

        externalIdentityRepository.deleteAll(externalIdentityRepository.findByUserId(id));
    }

    public User updateProfile(Long id, String displayName, String locale, String timeZone, String avatarUrl) {
        UserEntity entity = userRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("User with id " + id + " not found"));
        if (Boolean.FALSE.equals(entity.getActive())) {
            throw new AuthException("User account has been deactivated");
        }
        entity.setDisplayName(blank(displayName, 120));
        entity.setLocale(blank(locale, 35));
        entity.setTimeZone(blank(timeZone, 64));
        entity.setAvatarUrl(avatar(avatarUrl));
        return getUserById(userRepository.update(entity).getId());
    }

    private static String blank(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > max) {
            throw new AuthException("Value is too long");
        }
        return trimmed;
    }

    private static String avatar(String value) {
        String trimmed = blank(value, 2000);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.startsWith("https://")) {
            throw new AuthException("Avatar URL must use https");
        }
        return trimmed;
    }
}
