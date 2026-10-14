package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.crypto.PasswordHasher;
import com.posadskiy.auth.core.crypto.PasswordPolicy;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.time.Instant;

@Singleton
public class UserAccountService {

    private final UsersRepository usersRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final AuditService auditService;

    public UserAccountService(
            UsersRepository usersRepository,
            PasswordHasher passwordHasher,
            PasswordPolicy passwordPolicy,
            AuditService auditService) {
        this.usersRepository = usersRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.auditService = auditService;
    }

    @Transactional
    public UserEntity register(String email, String rawPassword, String username) {
        String normalized = EmailNormalizer.normalize(email);
        if (normalized == null || !normalized.contains("@")) {
            throw new AuthException("A valid email is required");
        }
        passwordPolicy.validate(rawPassword, normalized, username);
        if (usersRepository.findByEmailNormalized(normalized).isPresent()) {
            throw new AuthException("An account with that email already exists", 409);
        }
        UserEntity user = new UserEntity();
        user.setEmail(email.trim());
        user.setEmailNormalized(normalized);
        user.setUsername(uniqueUsername(username, normalized));
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setEmailVerified(Boolean.FALSE);
        user.setRoles("ROLE_USER");
        user.setActive(Boolean.TRUE);
        user.setPasswordChangedAt(Instant.now());
        UserEntity saved = usersRepository.save(user);
        auditService.record(saved.getId(), "USER_REGISTERED", "email");
        return saved;
    }

    public UserEntity authenticate(String identity, String rawPassword) {
        String lookup = identity.contains("@") ? EmailNormalizer.normalize(identity) : identity;
        UserEntity user = identity.contains("@")
                ? usersRepository.findByEmailNormalized(lookup).orElse(null)
                : usersRepository.findByUsername(lookup).orElse(null);
        if (user == null) {
            passwordHasher.dummyVerify(rawPassword);
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            passwordHasher.dummyVerify(rawPassword);
            throw new AuthException("Account is disabled", 403);
        }
        if (user.getPasswordHash() == null || !passwordHasher.verify(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setLastLoginAt(Instant.now());
        usersRepository.update(user);
        return user;
    }

    @Transactional
    public UserEntity changePassword(UserEntity user, String rawPassword) {
        passwordPolicy.validate(rawPassword, user.getEmailNormalized(), user.getUsername());
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setPasswordChangedAt(Instant.now());
        UserEntity updated = usersRepository.update(user);
        auditService.record(user.getId(), "PASSWORD_CHANGED", null);
        return updated;
    }

    public UserEntity requireActive(Long userId) {
        UserEntity user = usersRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AuthException("Account is disabled", 403);
        }
        return user;
    }

    public UserEntity markVerified(UserEntity user) {
        user.setEmailVerified(Boolean.TRUE);
        return usersRepository.update(user);
    }

    public UserEntity deactivate(UserEntity user) {
        user.setActive(Boolean.FALSE);
        user.setDeactivatedAt(Instant.now());
        UserEntity updated = usersRepository.update(user);
        auditService.record(user.getId(), "USER_DEACTIVATED", null);
        return updated;
    }

    private String uniqueUsername(String requested, String email) {
        String base = requested == null || requested.isBlank()
                ? email.substring(0, email.indexOf('@')).replaceAll("[^a-zA-Z0-9._-]", "")
                : requested.trim();
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int attempts = 0;
        while (usersRepository.findByUsername(candidate).isPresent()) {
            candidate = base + "_" + SecureTokens.randomUrl(3);
            if (++attempts > 8) {
                throw new AuthException("Unable to allocate a username");
            }
        }
        return candidate;
    }
}
