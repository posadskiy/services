package com.posadskiy.auth.core.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.posadskiy.auth.core.config.OAuthProviderProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.oauth.AppleClientSecretService;
import com.posadskiy.auth.core.oauth.DatabaseOAuthStateStore;
import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.oauth.IdTokenValidator;
import com.posadskiy.auth.core.oauth.OAuthOutboundClient;
import com.posadskiy.auth.core.oauth.OAuthProviderRegistry;
import com.posadskiy.auth.core.storage.db.entity.OAuthStateEntity;
import io.micronaut.core.util.StringUtils;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class OAuthTokenExchangeService {

    private final OAuthProviderRegistry registry;
    private final DatabaseOAuthStateStore stateStore;
    private final OAuthOutboundClient outboundClient;
    private final ObjectMapper objectMapper;
    private final IdTokenValidator idTokenValidator;
    private final AppleClientSecretService appleClientSecretService;

    public OAuthTokenExchangeService(
            OAuthProviderRegistry registry,
            DatabaseOAuthStateStore stateStore,
            OAuthOutboundClient outboundClient,
            ObjectMapper objectMapper,
            IdTokenValidator idTokenValidator,
            AppleClientSecretService appleClientSecretService) {
        this.registry = registry;
        this.stateStore = stateStore;
        this.outboundClient = outboundClient;
        this.objectMapper = objectMapper;
        this.idTokenValidator = idTokenValidator;
        this.appleClientSecretService = appleClientSecretService;
    }

    public ExchangeResult exchange(
            String providerPath, String code, String stateValue, String browserBinding, String appleUserJson) {
        OAuthStateEntity state =
                stateStore.consume(stateValue).orElseThrow(() -> new OAuthException("Invalid state value"));
        if (!state.provider().equalsIgnoreCase(providerPath)) {
            throw new OAuthException("OAuth provider mismatch");
        }
        if (!SecureTokens.equalsConstantTime(state.browserBindingHash(), SecureTokens.sha256Hex(browserBinding))) {
            throw new OAuthException("OAuth browser binding mismatch");
        }
        OAuthProviderProperties provider = registry.require(state.provider());

        Map<String, String> form = new HashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("code", code);
        form.put("redirect_uri", state.redirectUri());
        form.put("client_id", provider.clientId());
        form.put("client_secret", appleClientSecretService.clientSecret(provider));
        form.put("code_verifier", state.codeVerifier());

        Map<String, Object> tokenJson = readJson(outboundClient.postForm(provider.tokenUri(), form));
        String idToken = stringValue(tokenJson, "id_token");
        JWTClaimsSet claims = idTokenValidator.validate(provider, idToken, state.nonce());

        String subject = claims.getSubject();
        String email = claimString(claims, "email");
        boolean emailVerified = booleanClaim(claims, "email_verified");
        String name = claimString(claims, "name");
        String picture = claimString(claims, "picture");
        if (StringUtils.isEmpty(name) && appleUserJson != null) {
            name = readAppleName(appleUserJson);
        }
        if (StringUtils.isEmpty(email) && appleUserJson != null) {
            email = stringValue(readJson(appleUserJson), "email");
        }
        ExternalProfile profile = new ExternalProfile(state.provider(), subject, email, emailVerified, name, picture);
        return new ExchangeResult(profile, state.codeChallenge(), state.frontendRedirectUrl());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new OAuthException("Unable to parse provider response");
        }
    }

    private String readAppleName(String appleUserJson) {
        Map<String, Object> json = readJson(appleUserJson);
        Object name = json.get("name");
        if (name instanceof Map<?, ?> map) {
            Object first = map.get("firstName");
            Object last = map.get("lastName");
            String joined = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            return joined.isEmpty() ? null : joined;
        }
        return null;
    }

    private static String stringValue(Map<String, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String claimString(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean booleanClaim(JWTClaimsSet claims, String name) {
        Object value = claims.getClaim(name);
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value == null ? "false" : value));
    }

    public record ExchangeResult(ExternalProfile profile, String codeChallenge, String frontendRedirectUrl) {}
}
