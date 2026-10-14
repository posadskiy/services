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
}
