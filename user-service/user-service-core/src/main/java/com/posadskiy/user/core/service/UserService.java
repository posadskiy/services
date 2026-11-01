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

    public User getUserById(Long id) {
        var entity = userRepository
            .findById(id)
            .orElseThrow(() -> new AuthException("User with id " + id + " not found", 404));

        // Check if user is active
        if (!entity.active()) {
            throw new AuthException("User account has been deactivated", 403);
        }

        User baseUser = userEntityMapper.mapFromEntity(entity);
        List<String> providers = externalIdentityRepository.findByUserId(id).stream()
            .map(identity -> identity.provider().toLowerCase())
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
            .orElseThrow(() -> new AuthException("User with id " + id + " not found", 404));

        String normalized = existingEntity.emailNormalized();
        if (normalized != null && !normalized.endsWith("@deleted.local")) {
            accountCleanupRepository.suppress(normalized);
        }
        accountCleanupRepository.revokeTokens(id);
        accountCleanupRepository.deletePasskeys(id);
        accountCleanupRepository.deletePreferences(id);

        userRepository.update(existingEntity.erased(LocalDateTime.now()));

        externalIdentityRepository.deleteAll(externalIdentityRepository.findByUserId(id));
    }

    public User updateProfile(Long id, String displayName, String locale, String timeZone, String avatarUrl) {
        UserEntity entity = userRepository
            .findById(id)
            .orElseThrow(() -> new AuthException("User with id " + id + " not found", 404));
        if (!entity.active()) {
            throw new AuthException("User account has been deactivated", 403);
        }
        UserEntity updated = userRepository.update(
            entity.withProfile(
                blank(displayName, 120),
                RegionalPreference.locale(locale),
                RegionalPreference.timeZone(timeZone),
                avatar(avatarUrl)));
        return getUserById(updated.id());
    }
}
