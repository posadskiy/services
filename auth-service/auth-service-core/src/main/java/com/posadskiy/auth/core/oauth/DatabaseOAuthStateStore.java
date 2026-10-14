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

    public OAuthState create(
            String provider,
            String redirectUri,
            String codeVerifier,
            String nonce,
            String browserBinding,
            String codeChallenge) {
        String stateValue = SecureTokens.randomUrl(32);
        OAuthStateEntity entity = new OAuthStateEntity();
        entity.setStateValue(stateValue);
        entity.setProvider(provider);
        entity.setRedirectUri(redirectUri);
        entity.setCodeVerifier(codeVerifier);
        entity.setNonce(nonce);
        entity.setBrowserBindingHash(SecureTokens.sha256Hex(browserBinding));
        entity.setCodeChallenge(codeChallenge);
        entity.setExpiresAt(Instant.now().plus(properties.oauth().stateTtl()));
        repository.save(entity);
        return toState(entity);
    }

    public Optional<OAuthState> consume(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Optional<OAuthStateEntity> found = repository.findById(value);
        found.ifPresent(entity -> repository.deleteById(value));
        return found.filter(entity -> Instant.now().isBefore(entity.getExpiresAt()))
                .map(this::toState);
    }

    private OAuthState toState(OAuthStateEntity entity) {
        return new OAuthState(
                entity.getStateValue(),
                entity.getProvider(),
                entity.getRedirectUri(),
                entity.getCodeVerifier(),
                entity.getNonce(),
                entity.getBrowserBindingHash(),
                entity.getCodeChallenge(),
                entity.getExpiresAt());
    }
}
