package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.crypto.PasswordHasher;
import com.posadskiy.auth.core.crypto.PasswordPolicy;
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
    public UserEntity register(String email, String rawPassword) {
        String normalized = EmailNormalizer.normalize(email);
        if (normalized == null || !normalized.contains("@")) {
            throw new AuthException("A valid email is required");
        }
        passwordPolicy.validate(rawPassword, normalized);
        if (usersRepository.findByEmailNormalized(normalized).isPresent()) {
            throw new AuthException("An account with that email already exists", 409);
        }
        UserEntity saved = usersRepository.save(UserEntity.newAccount(
            email.trim(),
            normalized,
            passwordHasher.hash(rawPassword),
            false,
            null,
            Instant.now()));
        auditService.record(saved.id(), "USER_REGISTERED", "email");
        return saved;
    }

    public UserEntity authenticate(String email, String rawPassword) {
        String lookup = EmailNormalizer.normalize(email);
        UserEntity user = lookup == null ? null : usersRepository.findByEmailNormalized(lookup).orElse(null);
        if (user == null) {
            passwordHasher.dummyVerify(rawPassword);
            throw new InvalidCredentialsException();
        }
        if (!user.active()) {
            passwordHasher.dummyVerify(rawPassword);
            throw new AuthException("Account is disabled", 403);
        }
        if (user.passwordHash() == null || !passwordHasher.verify(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return usersRepository.update(user.loggedInAt(Instant.now()));
    }

    public UserEntity requireActive(Long userId) {
        UserEntity user = usersRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!user.active()) {
            throw new AuthException("Account is disabled", 403);
        }
        return user;
    }

    public UserEntity deactivate(UserEntity user) {
        UserEntity updated = usersRepository.update(user.deactivatedAt(Instant.now()));
        auditService.record(user.id(), "USER_DEACTIVATED", null);
        return updated;
    }
}
