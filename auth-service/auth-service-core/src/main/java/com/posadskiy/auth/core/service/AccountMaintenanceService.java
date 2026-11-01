package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.crypto.PasswordHasher;
import com.posadskiy.auth.core.crypto.PasswordPolicy;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import com.posadskiy.auth.core.storage.db.*;
import com.posadskiy.auth.core.storage.db.entity.*;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class AccountMaintenanceService {

    private final UsersRepository usersRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ExternalIdentityRepository identityRepository;
    private final PasskeyCredentialRepository passkeyRepository;
    private final WebAuthnChallengeRepository challengeRepository;
    private final ActionTokenRepository actionTokenRepository;
    private final AuditEventRepository auditEventRepository;
    private final PasswordHasher passwordHasher;
    private final PasswordPolicy passwordPolicy;
    private final AuditService auditService;

    public AccountMaintenanceService(
        UsersRepository usersRepository,
        RefreshTokenRepository refreshTokenRepository,
        ExternalIdentityRepository identityRepository,
        PasskeyCredentialRepository passkeyRepository,
        WebAuthnChallengeRepository challengeRepository,
        ActionTokenRepository actionTokenRepository,
        AuditEventRepository auditEventRepository,
        PasswordHasher passwordHasher,
        PasswordPolicy passwordPolicy,
        AuditService auditService) {
        this.usersRepository = usersRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.identityRepository = identityRepository;
        this.passkeyRepository = passkeyRepository;
        this.challengeRepository = challengeRepository;
        this.actionTokenRepository = actionTokenRepository;
        this.auditEventRepository = auditEventRepository;
        this.passwordHasher = passwordHasher;
        this.passwordPolicy = passwordPolicy;
        this.auditService = auditService;
    }

    public List<RefreshTokenEntity> sessions(Long userId) {
        Map<UUID, RefreshTokenEntity> byFamily = new LinkedHashMap<>();
        for (RefreshTokenEntity token : refreshTokenRepository.findByUserIdAndRevokedFalseAndExpiresAtGreaterThan(
            userId, Instant.now())) {
            RefreshTokenEntity current = byFamily.get(token.familyId());
            if (current == null || newer(token, current)) {
                byFamily.put(token.familyId(), token);
            }
        }
        return List.copyOf(byFamily.values());
    }

    private static boolean newer(RefreshTokenEntity candidate, RefreshTokenEntity current) {
        if (candidate.createdAt() == null) {
            return false;
        }
        return current.createdAt() == null || candidate.createdAt().isAfter(current.createdAt());
    }

    @Transactional
    public void revokeSession(Long userId, UUID familyId) {
        if (refreshTokenRepository.revokeFamilyForUser(familyId, userId) == 0) {
            throw new AuthException("Session not found", 404);
        }
        auditService.record(userId, "SESSION_REVOKED", familyId.toString());
    }

    @Transactional
    public void changePassword(UserEntity user, String currentPassword, String nextPassword) {
        if (user.passwordHash() == null || !passwordHasher.verify(currentPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        passwordPolicy.validate(nextPassword, user.email());
        usersRepository.update(user.withPassword(passwordHasher.hash(nextPassword), Instant.now()));
        refreshTokenRepository.revokeAllForUser(user.id());
        auditService.record(user.id(), "PASSWORD_CHANGED", null);
    }

    @Transactional
    public void setPassword(UserEntity user, String password) {
        if (user.passwordHash() != null) {
            throw new AuthException("This account already has a password", 409);
        }
        passwordPolicy.validate(password, user.email());
        usersRepository.update(user.withPassword(passwordHasher.hash(password), Instant.now()));
        auditService.record(user.id(), "PASSWORD_SET", null);
    }

    public List<ExternalIdentityEntity> identities(Long userId) {
        return identityRepository.findByUserId(userId).stream()
            .filter(identity -> !identity.revoked())
            .toList();
    }

    @Transactional
    public void unlink(UserEntity user, String provider) {
        List<ExternalIdentityEntity> live = identities(user.id());
        ExternalIdentityEntity match = live.stream()
            .filter(identity -> identity.provider().equalsIgnoreCase(provider))
            .findFirst()
            .orElseThrow(() -> new AuthException("That sign-in method is not linked", 404));
        long others = live.stream().filter(identity -> identity.id() != null && !identity.id().equals(match.id())).count();
        if (user.passwordHash() == null && others == 0 && passkeyRepository.countByUserId(user.id()) == 0) {
            throw new AuthException("Add a password, passkey, or another provider before unlinking", 409);
        }
        identityRepository.update(match.withRevoked());
        auditService.record(user.id(), "SOCIAL_UNLINKED", provider);
    }

    public Map<String, Object> export(UserEntity user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.id());
        body.put("email", user.email());
        body.put("emailVerified", user.emailVerified());
        body.put("pictureUrl", user.pictureUrl());
        body.put("createdAt", user.createdAt());
        body.put(
            "identities",
            identityRepository.findByUserId(user.id()).stream().map(this::identity).toList());
        body.put("sessions", sessions(user.id()).stream().map(this::session).toList());
        body.put(
            "passkeys",
            passkeyRepository.findByUserId(user.id()).stream().map(this::passkey).toList());
        body.put(
            "audit",
            auditEventRepository.findByUserIdOrderByCreatedAtDesc(user.id()).stream().map(this::audit).toList());
        return body;
    }

    @Transactional
    public void erase(UserEntity user, String password, String confirmation) {
        if (!"DELETE".equals(confirmation)) {
            throw new AuthException("Type DELETE to confirm account erasure");
        }
        if (user.passwordHash() != null
            && (password == null || !passwordHasher.verify(password, user.passwordHash()))) {
            throw new InvalidCredentialsException();
        }
        String normalized = EmailNormalizer.normalize(user.email());
        if (normalized != null) {
            usersRepository.suppressEmail(normalized);
        }
        refreshTokenRepository.revokeAllForUser(user.id());
        passkeyRepository.deleteByUserId(user.id());
        challengeRepository.deleteByUserId(user.id());
        identityRepository.deleteByUserId(user.id());
        actionTokenRepository.deleteByUserId(user.id());
        usersRepository.deletePreferences(user.id());
        usersRepository.update(user.erased(Instant.now()));
        usersRepository.clearProfile(user.id());
        auditService.record(user.id(), "ACCOUNT_ERASED", null);
    }

    private Map<String, Object> identity(ExternalIdentityEntity identity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("provider", identity.provider());
        row.put("email", identity.email());
        row.put("revoked", identity.revoked());
        return row;
    }

    private Map<String, Object> session(RefreshTokenEntity token) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", token.familyId().toString());
        row.put("provider", token.provider());
        row.put("userAgent", token.userAgent());
        row.put("clientIp", token.clientIp());
        row.put("createdAt", token.createdAt());
        return row;
    }

    private Map<String, Object> passkey(PasskeyCredentialEntity credential) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", credential.id());
        row.put("label", credential.label());
        row.put("createdAt", credential.createdAt());
        row.put("lastUsedAt", credential.lastUsedAt());
        return row;
    }

    private Map<String, Object> audit(AuditEventEntity event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", event.eventType());
        row.put("detail", event.detail());
        row.put("createdAt", event.createdAt());
        return row;
    }
}
