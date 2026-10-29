package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.storage.db.ActionTokenRepository;
import com.posadskiy.auth.core.storage.db.entity.ActionTokenEntity;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;

@Singleton
public class ActionTokenService {

    private final ActionTokenRepository actionTokenRepository;

    public ActionTokenService(ActionTokenRepository actionTokenRepository) {
        this.actionTokenRepository = actionTokenRepository;
    }

    @Transactional
    public Issued issue(Long userId, String purpose, Duration ttl) {
        Instant now = Instant.now();
        actionTokenRepository.consumeOpen(userId, purpose, now);
        String raw = SecureTokens.randomUrl(32);
        ActionTokenEntity saved = actionTokenRepository.save(
                new ActionTokenEntity(null, userId, purpose, SecureTokens.sha256Hex(raw), now.plus(ttl), null, null));
        return new Issued(saved.id(), raw);
    }

    @Transactional
    public ActionTokenEntity consume(String rawToken, String purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalid();
        }
        String hash = SecureTokens.sha256Hex(rawToken.trim());
        if (actionTokenRepository.consumeIfOpen(hash, purpose, Instant.now()) != 1) {
            throw invalid();
        }
        return actionTokenRepository.findByTokenHash(hash).orElseThrow(this::invalid);
    }

    private AuthException invalid() {
        return new AuthException("Invalid or expired token");
    }

    public record Issued(Long id, String raw) {}
}
