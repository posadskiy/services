package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.storage.db.ActionTokenRepository;
import com.posadskiy.auth.core.storage.db.entity.ActionTokenEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;

@Singleton
public class ActionTokenService {

    public static final String VERIFY_EMAIL = "VERIFY_EMAIL";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";

    private final ActionTokenRepository repository;
    private final MailService mailService;
    private final AuthProperties properties;

    public ActionTokenService(ActionTokenRepository repository, MailService mailService, AuthProperties properties) {
        this.repository = repository;
        this.mailService = mailService;
        this.properties = properties;
    }

    public void sendVerifyEmail(UserEntity user) {
        String token = issue(user.getId(), VERIFY_EMAIL, Duration.ofHours(24));
        String link = baseUrl() + "/account/verify?token=" + token;
        mailService.send(user.getEmail(), "Verify your email", "Open this link to verify your email: " + link);
    }

    public void sendPasswordReset(UserEntity user) {
        String token = issue(user.getId(), RESET_PASSWORD, Duration.ofHours(1));
        String link = baseUrl() + "/account/reset-password?token=" + token;
        mailService.send(user.getEmail(), "Reset your password", "Open this link to reset your password: " + link);
    }

    public ActionTokenEntity consume(String token, String purpose) {
        ActionTokenEntity entity = repository
                .findByTokenHash(SecureTokens.sha256Hex(token))
                .orElseThrow(() -> new AuthException("Invalid or expired token", 400));
        if (!purpose.equals(entity.getPurpose())
                || entity.getConsumedAt() != null
                || Instant.now().isAfter(entity.getExpiresAt())) {
            throw new AuthException("Invalid or expired token", 400);
        }
        entity.setConsumedAt(Instant.now());
        return repository.update(entity);
    }

    private String issue(Long userId, String purpose, Duration ttl) {
        String value = SecureTokens.randomUrl(32);
        ActionTokenEntity entity = new ActionTokenEntity();
        entity.setUserId(userId);
        entity.setPurpose(purpose);
        entity.setTokenHash(SecureTokens.sha256Hex(value));
        entity.setExpiresAt(Instant.now().plus(ttl));
        repository.save(entity);
        return value;
    }

    private String baseUrl() {
        String configured = properties.getMail().getPublicBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return properties.getOauth().getRedirectBaseUrl();
    }
}
