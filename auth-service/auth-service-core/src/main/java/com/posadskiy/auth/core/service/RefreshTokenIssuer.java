package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import jakarta.inject.Singleton;
import java.security.SecureRandom;
import java.util.Base64;

@Singleton
public class RefreshTokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenIssuer(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issue(Long userId, String provider, Long externalIdentityId) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUsername(String.valueOf(userId));
        entity.setRefreshToken(generateTokenValue());
        entity.setProvider(provider);
        entity.setExternalIdentityId(externalIdentityId);
        entity.setRevoked(Boolean.FALSE);
        refreshTokenRepository.save(entity);
        return entity.getRefreshToken();
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

