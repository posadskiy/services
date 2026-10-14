package com.posadskiy.auth.core.service;

import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.config.OAuthClientProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Singleton
public class TokenService {

    private final TokenGenerator tokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountService userAccountService;
    private final AuthProperties properties;
    private final List<OAuthClientProperties> clients;
    private final AuditService auditService;
    private final AuthMetrics metrics;

    public TokenService(
            TokenGenerator tokenGenerator,
            RefreshTokenRepository refreshTokenRepository,
            UserAccountService userAccountService,
            AuthProperties properties,
            List<OAuthClientProperties> clients,
            AuditService auditService,
            AuthMetrics metrics) {
        this.tokenGenerator = tokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountService = userAccountService;
        this.properties = properties;
        this.clients = clients;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    public TokenResponse issueForUser(UserEntity user, String provider, Long externalIdentityId) {
        String access = accessToken(user, provider, externalIdentityId);
        String refresh = persistRefresh(user.getId(), provider, externalIdentityId, UUID.randomUUID());
        return new TokenResponse(
                access, refresh, properties.jwt().accessTokenTtl().toSeconds(), "Bearer", user.getId());
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshTokenEntity existing = refreshTokenRepository
                .findByTokenHash(SecureTokens.sha256Hex(refreshToken))
                .orElseThrow(() -> {
                    metrics.refresh("unknown");
                    return new InvalidCredentialsException();
                });
        if (Boolean.TRUE.equals(existing.getRevoked()) || Instant.now().isAfter(existing.getExpiresAt())) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            metrics.refresh("revoked");
            auditService.record(existing.getUserId(), "REFRESH_REUSE_OR_EXPIRED", null);
            throw new InvalidCredentialsException();
        }
        UserEntity user = userAccountService.requireActive(existing.getUserId());
        existing.setRevoked(Boolean.TRUE);
        existing.setLastUsedAt(Instant.now());
        String next = persistRefresh(
                user.getId(), existing.getProvider(), existing.getExternalIdentityId(), existing.getFamilyId());
        RefreshTokenEntity replacement = refreshTokenRepository
                .findByTokenHash(SecureTokens.sha256Hex(next))
                .orElseThrow();
        existing.setReplacedById(replacement.getId());
        refreshTokenRepository.update(existing);
        metrics.refresh("success");
        return new TokenResponse(
                accessToken(user, existing.getProvider(), existing.getExternalIdentityId()),
                next,
                properties.jwt().accessTokenTtl().toSeconds(),
                "Bearer",
                user.getId());
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository
                .findByTokenHash(SecureTokens.sha256Hex(refreshToken))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        auditService.record(userId, "TOKENS_REVOKED", "all");
    }

    public TokenResponse clientCredentials(String clientId, String clientSecret) {
        OAuthClientProperties client = clients.stream()
                .filter(OAuthClientProperties::isEnabled)
                .filter(c -> c.getClientId().equals(clientId))
                .findFirst()
                .orElseThrow(InvalidCredentialsException::new);
        if (client.getClientSecret() == null
                || !SecureTokens.equalsConstantTime(client.getClientSecret(), clientSecret)) {
            throw new InvalidCredentialsException();
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("token_use", "access");
        attributes.put("client_id", clientId);
        Authentication authentication = Authentication.build(clientId, client.getRoles(), attributes);
        int ttl = (int) properties.jwt().accessTokenTtl().toSeconds();
        String access = tokenGenerator
                .generateToken(authentication, ttl)
                .orElseThrow(() -> new IllegalStateException("Unable to generate access token"));
        return new TokenResponse(access, null, ttl, "Bearer", null);
    }

    private String accessToken(UserEntity user, String provider, Long externalIdentityId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", user.getEmail());
        attributes.put("email_verified", user.getEmailVerified());
        if (user.getPictureUrl() != null) {
            attributes.put("picture", user.getPictureUrl());
        }
        if (provider != null) {
            attributes.put("idp_provider", provider);
        }
        if (externalIdentityId != null) {
            attributes.put("external_identity_id", externalIdentityId);
        }
        Authentication authentication = Authentication.build(String.valueOf(user.getId()), user.roleList(), attributes);
        int ttl = (int) properties.jwt().accessTokenTtl().toSeconds();
        return tokenGenerator
                .generateToken(authentication, ttl)
                .orElseThrow(() -> new IllegalStateException("Unable to generate access token"));
    }

    private String persistRefresh(Long userId, String provider, Long externalIdentityId, UUID familyId) {
        String value = SecureTokens.randomUrl(64);
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(SecureTokens.sha256Hex(value));
        entity.setFamilyId(familyId);
        entity.setProvider(provider);
        entity.setExternalIdentityId(externalIdentityId);
        entity.setRevoked(Boolean.FALSE);
        entity.setExpiresAt(Instant.now().plus(properties.token().refreshTokenTtl()));
        refreshTokenRepository.save(entity);
        return value;
    }
}
