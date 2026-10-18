package com.posadskiy.auth.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.posadskiy.auth.api.dto.LoginRequest;
import com.posadskiy.auth.api.dto.RefreshRequest;
import com.posadskiy.auth.api.dto.RegisterRequest;
import com.posadskiy.auth.api.dto.RevokeRequest;
import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.api.dto.UserResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@MicronautTest
class AuthFlowTest extends PostgresMicronautTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void registerLoginRefreshAndRevoke() {
        UserResponse created = client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/users", new RegisterRequest("flow@example.com", "correcthorse", "flowuser")),
                        UserResponse.class);
        assertEquals("flow@example.com", created.email());
        assertEquals("flowuser", created.username());

        TokenResponse tokens = client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/login", new LoginRequest("flow@example.com", "correcthorse")),
                        TokenResponse.class);
        assertNotNull(tokens.accessToken());
        assertNotNull(tokens.refreshToken());
        assertTrue(tokens.expiresIn() <= 900);

        HttpClientResponseException denied = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.POST("/login", new LoginRequest("flow@example.com", "wrong-password"))));
        assertEquals(HttpStatus.UNAUTHORIZED, denied.getStatus());

        TokenResponse refreshed = client.toBlocking()
                .retrieve(
                        HttpRequest.POST(
                                "/oauth/token",
                                new RefreshRequest("refresh_token", tokens.refreshToken(), null, null, null, null)),
                        TokenResponse.class);
        assertNotNull(refreshed.accessToken());
        assertNotEquals(tokens.refreshToken(), refreshed.refreshToken());

        HttpClientResponseException reuse = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.POST(
                                "/oauth/token",
                                new RefreshRequest("refresh_token", tokens.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.getStatus());

        client.toBlocking().exchange(HttpRequest.POST("/oauth/revoke", new RevokeRequest(refreshed.refreshToken())));

        HttpClientResponseException afterRevoke = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.POST(
                                "/oauth/token",
                                new RefreshRequest(
                                        "refresh_token", refreshed.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, afterRevoke.getStatus());
    }

    @Test
    void formRefreshRotatesTheToken() {
        client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/users", new RegisterRequest("form@example.com", "correcthorse", "formuser")),
                        UserResponse.class);
        TokenResponse tokens = client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/login", new LoginRequest("form@example.com", "correcthorse")),
                        TokenResponse.class);

        TokenResponse refreshed = client.toBlocking()
                .retrieve(
                        HttpRequest.POST(
                                        "/oauth/token",
                                        "grant_type=refresh_token&refresh_token=" + tokens.refreshToken())
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED),
                        TokenResponse.class);
        assertNotNull(refreshed.accessToken());
        assertNotEquals(tokens.refreshToken(), refreshed.refreshToken());
    }

    @Test
    void clientCredentialsIssuesAccessTokenWithoutRefresh() {
        TokenResponse tokens = client.toBlocking()
                .retrieve(
                        HttpRequest.POST(
                                "/oauth/token",
                                new RefreshRequest("client_credentials", null, "system", "system-secret", null, null)),
                        TokenResponse.class);
        assertNotNull(tokens.accessToken());
        assertEquals(null, tokens.refreshToken());
    }

    @Test
    void duplicateEmailIsConflict() {
        client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/users", new RegisterRequest("dup@example.com", "correcthorse", "dup1")),
                        UserResponse.class);
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.POST(
                                "/users", new RegisterRequest("DUP@example.com", "correcthorse", "dup2"))));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void systemPasswordLoginIsRejected() {
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.POST("/login", new LoginRequest("system", "anypassword"))));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void accountMeRequiresBearerAndReturnsProfile() {
        client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/users", new RegisterRequest("me@example.com", "correcthorse", "meuser")),
                        UserResponse.class);
        TokenResponse tokens = client.toBlocking()
                .retrieve(
                        HttpRequest.POST("/login", new LoginRequest("me@example.com", "correcthorse")),
                        TokenResponse.class);

        HttpClientResponseException anonymous = assertThrows(
                HttpClientResponseException.class, () -> client.toBlocking().retrieve(HttpRequest.GET("/account/me")));
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getStatus());

        UserResponse me = client.toBlocking()
                .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);
        assertEquals("me@example.com", me.email());
        assertEquals("meuser", me.username());
    }

    @Test
    void discoveryAndKeysArePublic() {
        String discovery =
                client.toBlocking().retrieve(HttpRequest.GET("/.well-known/openid-configuration"), String.class);
        assertTrue(discovery.contains("https://auth.test"));
        assertTrue(discovery.contains("refresh_token"));

        io.micronaut.http.HttpResponse<String> keys =
                client.toBlocking().exchange(HttpRequest.GET("/keys"), String.class);
        assertEquals(HttpStatus.OK, keys.getStatus());
        assertNotNull(keys.body());
        assertTrue(keys.getHeaders().contains("X-Content-Type-Options"));
    }

    @Test
    void oauthAuthorizeStartsPkceFlow() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/oauth2/authorize/google").accept(MediaType.APPLICATION_JSON), String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        String body = response.body();
        assertTrue(body.contains("accounts.google.com"));
        assertTrue(body.contains("code_challenge"));
        assertTrue(body.contains("test-google-client"));
        assertTrue(response.getHeaders().getAll("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("oauth_browser=") && value.contains("Path=/oauth2")));
    }

    @Test
    void oauthAuthorizeRedirectsABrowserAndScopesTheBindingCookie() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
                .exchange(
                        HttpRequest.GET("/oauth2/authorize/google?return_to=https://grammarbar.com/oauth/callback")
                                .accept(MediaType.TEXT_HTML),
                        String.class);
        assertTrue(response.getStatus().getCode() >= 300 && response.getStatus().getCode() < 400);
        assertTrue(response.getHeaders().get("Location").contains("accounts.google.com"));
        assertTrue(response.getHeaders().get("Location").contains("redirect_uri="));
        assertTrue(response.getHeaders().getAll("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("oauth_browser=") && value.contains("Path=/oauth2")));
    }

    @Test
    void oauthAuthorizeRejectsReturnUrlOutsideAllowlist() {
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.GET(
                                "/oauth2/authorize/google?return_to=https://evil.example/oauth/callback")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void oauthAuthorizeAcceptsAllowlistedReturnUrl() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
                .exchange(
                        HttpRequest.GET("/oauth2/authorize/google?return_to=https://costy.posadskiy.com/oauth/callback")
                                .accept(MediaType.APPLICATION_JSON),
                        String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.body().contains("accounts.google.com"));
    }

    @Test
    void unknownOauthProviderIsRejected() {
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(HttpRequest.GET("/oauth2/authorize/facebook")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void weakPasswordIsRejected() {
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(
                                HttpRequest.POST("/users", new RegisterRequest("weak@example.com", "short", "weak"))));
        assertTrue(ex.getStatus().getCode() >= 400);
    }

    @Test
    void clientCredentialsRejectsUnknownClient() {
        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking()
                        .retrieve(HttpRequest.POST(
                                "/oauth/token",
                                new RefreshRequest("client_credentials", null, "nope", "wrong", null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
