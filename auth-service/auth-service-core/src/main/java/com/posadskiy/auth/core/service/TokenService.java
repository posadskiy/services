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
    private final RefreshTokenRevoker refreshTokenRevoker;
    private final AccountMailService accountMailService;

    public TokenService(
        TokenGenerator tokenGenerator,
        RefreshTokenRepository refreshTokenRepository,
        UserAccountService userAccountService,
        AuthProperties properties,
        List<OAuthClientProperties> clients,
        AuditService auditService,
        AuthMetrics metrics,
        RefreshTokenRevoker refreshTokenRevoker,
        AccountMailService accountMailService) {
        this.tokenGenerator = tokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountService = userAccountService;
        this.properties = properties;
        this.clients = clients;
        this.auditService = auditService;
        this.metrics = metrics;
        this.refreshTokenRevoker = refreshTokenRevoker;
        this.accountMailService = accountMailService;
    }

    private static boolean secretConfigured(String clientSecret) {
        return clientSecret != null && !clientSecret.isBlank();
    }

    public TokenResponse issueForUser(UserEntity user, String provider, Long externalIdentityId) {
        return issueForUser(user, provider, externalIdentityId, null);
    }

    public TokenResponse issueForUser(
        UserEntity user, String provider, Long externalIdentityId, ClientContext client) {
        String access = accessToken(user, provider, externalIdentityId);
        boolean newDevice = client != null && isNewDevice(user.id(), client.userAgent());
        String refresh = persistRefresh(
            user.id(),
            provider,
            externalIdentityId,
            UUID.randomUUID(),
            client == null ? null : client.userAgent(),
            client == null ? null : client.ip());
        if (newDevice) {
            accountMailService.sendNewDevice(user, client);
        }
        return new TokenResponse(
            access, refresh, properties.jwt().accessTokenTtl().toSeconds(), "Bearer", UserResponse.idOf(user.id()));
    }

    /**
     * A sign-in is "new" only when the account has signed in before and never from this user agent.
     * The first sign-in is not announced, and a request without a user agent cannot be told apart.
     */
    private boolean isNewDevice(Long userId, String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return false;
        }
        return refreshTokenRepository.existsByUserId(userId)
            && !refreshTokenRepository.existsByUserIdAndUserAgent(userId, userAgent);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        String hash = SecureTokens.sha256Hex(refreshToken);
        Instant now = Instant.now();
        if (refreshTokenRepository.consumeIfCurrent(hash, now) == 1) {
            RefreshTokenEntity existing =
                refreshTokenRepository.findByTokenHash(hash).orElseThrow(InvalidCredentialsException::new);
            UserEntity user = userAccountService.requireActive(existing.userId());
            String next = persistRefresh(
                user.id(),
                existing.provider(),
                existing.externalIdentityId(),
                existing.familyId(),
                existing.userAgent(),
                existing.clientIp());
            RefreshTokenEntity replacement = refreshTokenRepository
                .findByTokenHash(SecureTokens.sha256Hex(next))
                .orElseThrow();
            refreshTokenRepository.update(existing.consumed(now, replacement.id()));
            metrics.refresh("success");
            return new TokenResponse(
                accessToken(user, existing.provider(), existing.externalIdentityId()),
                next,
                properties.jwt().accessTokenTtl().toSeconds(),
                "Bearer",
                UserResponse.idOf(user.id()));
        }
        var existing = refreshTokenRepository.findByTokenHash(hash);
        if (existing.isEmpty()) {
            metrics.refresh("unknown");
            throw new InvalidCredentialsException();
        }
        if (withinReuseGrace(existing.get(), now)) {
            return reissueWithinGrace(existing.get());
        }
        refreshTokenRevoker.revokeFamily(
            existing.get().familyId(), existing.get().userId());
        metrics.refresh("revoked");
        throw new InvalidCredentialsException();
    }

    /**
     * Two requests holding the same refresh token (two tabs, an app resuming) must not log the user out. A token that
     * was rotated a moment ago, whose replacement is still unused, is answered with one more token in the same family.
     * Once the window has passed, or the replacement has been used or revoked, presenting it is treated as theft.
     */
    private boolean withinReuseGrace(RefreshTokenEntity token, Instant now) {
        var grace = properties.token().reuseGrace();
        if (grace == null
            || grace.isZero()
            || grace.isNegative()
            || token.replacedById() == null
            || token.lastUsedAt() == null
            || !token.expiresAt().isAfter(now)
            || token.lastUsedAt().plus(grace).isBefore(now)) {
            return false;
        }
        return refreshTokenRepository
            .findById(token.replacedById())
            .map(replacement -> !replacement.revoked())
            .orElse(false);
    }

    private TokenResponse reissueWithinGrace(RefreshTokenEntity used) {
        UserEntity user = userAccountService.requireActive(used.userId());
        String next = persistRefresh(
            user.id(),
            used.provider(),
            used.externalIdentityId(),
            used.familyId(),
            used.userAgent(),
            used.clientIp());
        metrics.refresh("grace");
        return new TokenResponse(
            accessToken(user, used.provider(), used.externalIdentityId()),
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

    private String persistRefresh(
        Long userId, String provider, Long externalIdentityId, UUID familyId, String userAgent, String clientIp) {
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
            userAgent,
            clientIp,
            null));
        return value;
    }
}
