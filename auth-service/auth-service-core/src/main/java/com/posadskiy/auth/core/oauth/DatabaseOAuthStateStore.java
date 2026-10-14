package com.posadskiy.auth.core.oauth;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.storage.db.OAuthStateRepository;
import com.posadskiy.auth.core.storage.db.entity.OAuthStateEntity;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;

@Singleton
public class DatabaseOAuthStateStore {

    private final OAuthStateRepository repository;
    private final AuthProperties properties;

    public DatabaseOAuthStateStore(OAuthStateRepository repository, AuthProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public OAuthStateEntity create(
            String provider,
            String redirectUri,
            String codeVerifier,
            String nonce,
            String browserBinding,
            String codeChallenge,
            String frontendRedirectUrl) {
        OAuthStateEntity entity = new OAuthStateEntity(
                SecureTokens.randomUrl(32),
                provider,
                redirectUri,
                codeVerifier,
                nonce,
                SecureTokens.sha256Hex(browserBinding),
                codeChallenge,
                frontendRedirectUrl,
                Instant.now().plus(properties.oauth().stateTtl()),
                null);
        return repository.save(entity);
    }

    public Optional<OAuthStateEntity> consume(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Optional<OAuthStateEntity> found = repository.findById(value);
        found.ifPresent(entity -> repository.deleteById(value));
        return found.filter(entity -> Instant.now().isBefore(entity.expiresAt()));
    }
}
