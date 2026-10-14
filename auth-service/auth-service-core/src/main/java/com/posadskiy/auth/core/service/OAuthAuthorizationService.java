package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.config.OAuthProviderProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.oauth.DatabaseOAuthStateStore;
import com.posadskiy.auth.core.oauth.FrontendReturnUrls;
import com.posadskiy.auth.core.oauth.OAuthProviderRegistry;
import com.posadskiy.auth.core.oauth.Pkce;
import com.posadskiy.auth.core.storage.db.entity.OAuthStateEntity;
import io.micronaut.http.uri.UriBuilder;
import jakarta.inject.Singleton;
import java.util.List;

@Singleton
public class OAuthAuthorizationService {

    private final OAuthProviderRegistry registry;
    private final DatabaseOAuthStateStore stateStore;
    private final AuthProperties properties;

    public OAuthAuthorizationService(
            OAuthProviderRegistry registry, DatabaseOAuthStateStore stateStore, AuthProperties properties) {
        this.registry = registry;
        this.stateStore = stateStore;
        this.properties = properties;
    }

    public AuthorizationRequest authorize(
            String providerName, String browserBinding, String codeChallenge, String returnTo) {
        OAuthProviderProperties provider = registry.require(providerName);
        String redirectUri = provider.redirectUri();
        if (redirectUri == null || (!redirectUri.startsWith("http://") && !redirectUri.startsWith("https://"))) {
            throw new OAuthException("Redirect URI is not configured for provider " + providerName);
        }
        String frontendRedirect = FrontendReturnUrls.resolve(
                returnTo,
                properties.oauth().frontendRedirectUrl(),
                properties.oauth().allowedFrontendRedirectUrls());
        String codeVerifier = Pkce.verifier();
        String nonce = SecureTokens.randomUrl(32);
        OAuthStateEntity state = stateStore.create(
                providerName, redirectUri, codeVerifier, nonce, browserBinding, codeChallenge, frontendRedirect);

        var builder = UriBuilder.of(provider.authorizationUri())
                .queryParam("client_id", provider.clientId())
                .queryParam("response_type", "code")
                .queryParam("scope", formatScopes(provider.scopes()))
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state.stateValue())
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", Pkce.challengeS256(codeVerifier))
                .queryParam("code_challenge_method", "S256");
        if ("apple".equalsIgnoreCase(providerName)) {
            builder.queryParam("response_mode", "form_post");
        }
        return new AuthorizationRequest(builder.build().toString(), state.stateValue(), nonce);
    }

    private String formatScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "openid email profile";
        }
        return String.join(" ", scopes);
    }

    public record AuthorizationRequest(String authorizationUri, String state, String nonce) {}
}
