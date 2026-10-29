package com.posadskiy.auth.core.service;

import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.api.dto.UserResponse;
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

    private static boolean secretConfigured(String clientSecret) {
        return clientSecret != null && !clientSecret.isBlank();
    }

    public TokenResponse issueForUser(UserEntity user, String provider, Long externalIdentityId) {
        String access = accessToken(user, provider, externalIdentityId);
        String refresh = persistRefresh(user.id(), provider, externalIdentityId, UUID.randomUUID());
        return new TokenResponse(
            access, refresh, properties.jwt().accessTokenTtl().toSeconds(), "Bearer", UserResponse.idOf(user.id()));
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshTokenEntity existing = refreshTokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(refreshToken))
            .orElseThrow(() -> {
                metrics.refresh("unknown");
                return new InvalidCredentialsException();
            });
        if (existing.revoked() || Instant.now().isAfter(existing.expiresAt())) {
            refreshTokenRepository.revokeFamily(existing.familyId());
            metrics.refresh("revoked");
            auditService.record(existing.userId(), "REFRESH_REUSE_OR_EXPIRED", null);
            throw new InvalidCredentialsException();
        }
        UserEntity user = userAccountService.requireActive(existing.userId());
        String next =
            persistRefresh(user.id(), existing.provider(), existing.externalIdentityId(), existing.familyId());
        RefreshTokenEntity replacement = refreshTokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(next))
            .orElseThrow();
        refreshTokenRepository.update(existing.consumed(Instant.now(), replacement.id()));
        metrics.refresh("success");
        return new TokenResponse(
            accessToken(user, existing.provider(), existing.externalIdentityId()),
            next,
            properties.jwt().accessTokenTtl().toSeconds(),
            "Bearer",
            UserResponse.idOf(user.id()));
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository
            .findByTokenHash(SecureTokens.sha256Hex(refreshToken))
            .ifPresent(token -> refreshTokenRepository.revokeFamily(token.familyId()));
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
        auditService.record(userId, "TOKENS_REVOKED", "all");
    }

    public TokenResponse clientCredentials(String clientId, String clientSecret) {
        OAuthClientProperties client = clients.stream()
            .filter(OAuthClientProperties::enabled)
            .filter(c -> c.clientId().equals(clientId))
            .findFirst()
            .orElseThrow(InvalidCredentialsException::new);
        if (!secretConfigured(client.clientSecret())
            || clientSecret == null
            || clientSecret.isBlank()
            || !SecureTokens.equalsConstantTime(client.clientSecret(), clientSecret)) {
            throw new InvalidCredentialsException();
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("token_use", "access");
        attributes.put("client_id", clientId);
        Authentication authentication = Authentication.build(clientId, client.roles(), attributes);
        int ttl = (int) properties.jwt().accessTokenTtl().toSeconds();
        String access = tokenGenerator
            .generateToken(authentication, ttl)
            .orElseThrow(() -> new IllegalStateException("Unable to generate access token"));
        return new TokenResponse(access, null, ttl, "Bearer", null);
    }

    private String accessToken(UserEntity user, String provider, Long externalIdentityId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", user.email());
        attributes.put("email_verified", user.emailVerified());
        if (user.pictureUrl() != null) {
            attributes.put("picture", user.pictureUrl());
        }
        if (provider != null) {
            attributes.put("idp_provider", provider);
        }
        if (externalIdentityId != null) {
            attributes.put("external_identity_id", externalIdentityId);
        }
        Authentication authentication = Authentication.build(String.valueOf(user.id()), user.roleList(), attributes);
        int ttl = (int) properties.jwt().accessTokenTtl().toSeconds();
        return tokenGenerator
            .generateToken(authentication, ttl)
            .orElseThrow(() -> new IllegalStateException("Unable to generate access token"));
    }

    private String persistRefresh(Long userId, String provider, Long externalIdentityId, UUID familyId) {
        String value = SecureTokens.randomUrl(64);
        refreshTokenRepository.save(new RefreshTokenEntity(
            null,
            userId,
            SecureTokens.sha256Hex(value),
            familyId,
            null,
            provider,
            externalIdentityId,
            false,
            Instant.now().plus(properties.token().refreshTokenTtl()),
            null,
            null));
        return value;
    }
}
