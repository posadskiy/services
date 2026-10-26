package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.ExternalIdentityRepository;
import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.exception.AuthException;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
public class UserService {
    private final UserEntityMapper userEntityMapper;
    private final UsersRepository userRepository;
    private final ExternalIdentityRepository externalIdentityRepository;

    public UserService(
        UserEntityMapper userEntityMapper,
        UsersRepository userRepository,
        ExternalIdentityRepository externalIdentityRepository) {
        this.userEntityMapper = userEntityMapper;
        this.userRepository = userRepository;
        this.externalIdentityRepository = externalIdentityRepository;
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
            baseUser.username(),
            baseUser.email(),
            baseUser.password(),
            baseUser.emailVerified(),
            baseUser.pictureUrl(),
            baseUser.createdVia(),
            providers,
            baseUser.active(),
            baseUser.deactivatedAt());
    }

    public User updateUser(Long id, String username) {
        UserEntity existingEntity = userRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("User with id " + id + " not found"));

        // Check if user is active
        if (Boolean.FALSE.equals(existingEntity.getActive())) {
            throw new AuthException("Cannot update deactivated user account");
        }

        // Only username can be updated
        if (username == null || username.isBlank()) {
            throw new AuthException("Username is required and cannot be blank");
        }

        // Validate username uniqueness if username is being changed
        // Only check against active users (inactive users can reuse usernames)
        if (!username.equals(existingEntity.getUsername())) {
            Optional<UserEntity> existingByUsername = userRepository.findByUsernameAndActive(username, Boolean.TRUE);
            if (existingByUsername.isPresent() && !existingByUsername.get().getId().equals(id)) {
                throw new AuthException("User with username " + username + " already exists");
            }
            existingEntity.setUsername(username);
        }

        UserEntity savedEntity = userRepository.update(existingEntity);
        User baseUser = userEntityMapper.mapFromEntity(savedEntity);
        List<String> providers = externalIdentityRepository.findByUserId(id).stream()
            .map(identity -> identity.getProvider().toLowerCase())
            .distinct()
            .collect(Collectors.toList());
        return new User(
            baseUser.id(),
            baseUser.username(),
            baseUser.email(),
            baseUser.password(),
            baseUser.emailVerified(),
            baseUser.pictureUrl(),
            baseUser.createdVia(),
            providers,
            baseUser.active(),
            baseUser.deactivatedAt());
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

        // Anonymize user data (GDPR-compliant while preserving for analytics)
        existingEntity.setEmail("deleted_user_" + id + "@deleted.local");
        existingEntity.setUsername("deleted_user_" + id);
        existingEntity.setPasswordHash(null);
        existingEntity.setPictureUrl(null);
        existingEntity.setEmailVerified(Boolean.FALSE);
        existingEntity.setActive(Boolean.FALSE);
        existingEntity.setDeactivatedAt(LocalDateTime.now());
        userRepository.update(existingEntity);

        // Delete external identities (contain PII)
        externalIdentityRepository.deleteAll(externalIdentityRepository.findByUserId(id));
    }
}
