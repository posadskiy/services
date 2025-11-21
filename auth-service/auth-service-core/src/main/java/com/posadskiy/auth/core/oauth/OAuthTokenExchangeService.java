package com.posadskiy.auth.core.oauth;

import io.micronaut.core.util.StringUtils;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class OAuthTokenExchangeService {

    private final OAuthProviderRegistry registry;
    private final OAuthStateStore stateStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OAuthTokenExchangeService(
            OAuthProviderRegistry registry, OAuthStateStore stateStore, ObjectMapper objectMapper) {
        this.registry = registry;
        this.stateStore = stateStore;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    public ExternalProfile exchange(String providerName, String code, String stateValue) {
        OAuthProviderConfigurationProperties provider = registry.require(providerName);
        OAuthState state =
                stateStore.consume(stateValue).orElseThrow(() -> new IllegalArgumentException("Invalid state value"));

        Map<String, String> formParams = new HashMap<>();
        formParams.put("grant_type", "authorization_code");
        formParams.put("code", code);
        formParams.put("redirect_uri", state.redirectUri());
        formParams.put("client_id", provider.getClientId());
        if (StringUtils.isNotEmpty(provider.getClientSecret())) {
            formParams.put("client_secret", provider.getClientSecret());
        }
        formParams.put("code_verifier", state.codeVerifier());

        String formBody = buildForm(formParams);
        HttpRequest tokenRequest =
                HttpRequest.newBuilder(URI.create(provider.getTokenUri()))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build();

        TokenResponse tokenResponse = executeTokenRequest(tokenRequest);
        Map<String, Object> profileClaims = fetchUserInfo(provider, tokenResponse.accessToken());

        String subject = readAsString(profileClaims, "sub");
        if (subject == null) {
            subject = readAsString(profileClaims, "id");
        }
        String email = readAsString(profileClaims, "email");
        boolean emailVerified =
                Boolean.parseBoolean(String.valueOf(profileClaims.getOrDefault("email_verified", Boolean.FALSE)));
        String name = readAsString(profileClaims, "name");
        if (StringUtils.isEmpty(name)) {
            name = readAsString(profileClaims, "given_name");
        }
        String picture = readAsString(profileClaims, "picture");
        Instant expiresAt = tokenResponse.expiresIn() != null
                ? Instant.now().plusSeconds(tokenResponse.expiresIn())
                : null;

        return new ExternalProfile(
                providerName,
                subject,
                email,
                emailVerified,
                name,
                picture,
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.idToken(),
                expiresAt,
                tokenResponse.raw());
    }

    @SuppressWarnings("unchecked")
    private TokenResponse executeTokenRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Token endpoint failed: " + response.body());
            }
            Map<String, Object> json = objectMapper.readValue(response.body(), Map.class);
            return new TokenResponse(
                    readAsString(json, "access_token"),
                    readAsString(json, "refresh_token"),
                    readAsString(json, "id_token"),
                    json.containsKey("expires_in") ? ((Number) json.get("expires_in")).longValue() : null,
                    readAsString(json, "token_type"),
                    response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to contact token endpoint", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to contact token endpoint", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(OAuthProviderConfigurationProperties provider, String accessToken) {
        if (StringUtils.isEmpty(provider.getUserInfoUri())) {
            return Map.of();
        }
        HttpRequest request =
                HttpRequest.newBuilder(URI.create(provider.getUserInfoUri()))
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/json")
                        .GET()
                        .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("User info endpoint failed: " + response.body());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to fetch user info", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to fetch user info", e);
        }
    }

    private String buildForm(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String readAsString(Map<String, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private record TokenResponse(
            String accessToken, String refreshToken, String idToken, Long expiresIn, String tokenType, String raw) {}
}

