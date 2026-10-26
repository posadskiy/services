package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.oauth.Pkce;
import com.posadskiy.auth.core.storage.db.OAuthLoginCodeRepository;
import com.posadskiy.auth.core.storage.db.entity.OAuthLoginCodeEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import jakarta.inject.Singleton;

import java.time.Instant;

@Singleton
public class OAuthLoginCodeService {

    private final OAuthLoginCodeRepository repository;
    private final AuthProperties properties;

    public OAuthLoginCodeService(OAuthLoginCodeRepository repository, AuthProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public String create(UserEntity user, Long identityId, String codeChallenge) {
        String code = SecureTokens.randomUrl(32);
        repository.save(new OAuthLoginCodeEntity(
            SecureTokens.sha256Hex(code),
            user.id(),
            identityId,
            codeChallenge,
            Instant.now().plus(properties.oauth().loginCodeTtl()),
            null));
        return code;
    }

    public OAuthLoginCodeEntity consume(String code, String codeVerifier) {
        OAuthLoginCodeEntity entity = repository
            .findById(SecureTokens.sha256Hex(code))
            .orElseThrow(() -> new OAuthException("Invalid login code"));
        repository.deleteById(entity.codeHash());
        if (Instant.now().isAfter(entity.expiresAt())) {
            throw new OAuthException("Login code expired");
        }
        if (entity.codeChallenge() != null && !Pkce.matches(codeVerifier, entity.codeChallenge())) {
            throw new OAuthException("Invalid code verifier");
        }
        return entity;
    }
}
