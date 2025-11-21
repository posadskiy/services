package com.posadskiy.auth.core.oauth;

import io.micronaut.core.util.StringUtils;
import io.micronaut.http.uri.UriBuilder;
import jakarta.inject.Singleton;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class OAuthAuthorizationService {

    private final OAuthProviderRegistry registry;
    private final OAuthStateStore stateStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthAuthorizationService(OAuthProviderRegistry registry, OAuthStateStore stateStore) {
        this.registry = registry;
        this.stateStore = stateStore;
    }

    public AuthorizationRequest authorize(String providerName, String overrideRedirectUri) {
        OAuthProviderConfigurationProperties provider = registry.require(providerName);
        String redirectUri =
                StringUtils.isNotEmpty(overrideRedirectUri) ? overrideRedirectUri : provider.getRedirectUri();

        String codeVerifier = generateRandomCodeVerifier();
        String codeChallenge = createCodeChallenge(codeVerifier);
        String nonce = generateNonce();

        OAuthState state = stateStore.create(providerName, redirectUri, codeVerifier, nonce);

        URI authorizationUri =
                UriBuilder.of(provider.getAuthorizationUri())
                        .queryParam("client_id", provider.getClientId())
                        .queryParam("response_type", "code")
                        .queryParam("scope", formatScopes(provider.getScopes()))
                        .queryParam("redirect_uri", redirectUri)
                        .queryParam("state", state.value())
                        .queryParam("nonce", nonce)
                        .queryParam("access_type", "offline")
                        .queryParam("code_challenge", codeChallenge)
                        .queryParam("code_challenge_method", "S256")
                        .build();

        return new AuthorizationRequest(authorizationUri.toString(), state.value(), nonce);
    }

    private String formatScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "openid email profile";
        }
        return scopes.stream().collect(Collectors.joining(" "));
    }

    private String generateRandomCodeVerifier() {
        byte[] buffer = new byte[32];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create PKCE challenge", e);
        }
    }

    private String generateNonce() {
        return generateRandomCodeVerifier();
    }

    public record AuthorizationRequest(String authorizationUri, String state, String nonce) {}
}

