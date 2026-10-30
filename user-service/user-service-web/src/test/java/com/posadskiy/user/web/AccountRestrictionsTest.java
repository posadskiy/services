package com.posadskiy.user.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.posadskiy.user.api.UserDto;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@MicronautTest
class AccountRestrictionsTest {

    private static final Map<String, String> LINK = Map.of(
            "provider", "google",
            "providerUserId", "victim-sub",
            "email", "victim@example.com");

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenGenerator tokenGenerator;

    private String token(String subject, String role) {
        return tokenGenerator
                .generateToken(Authentication.build(subject, List.of(role)), 300)
                .orElseThrow();
    }

    private HttpStatus statusOf(HttpRequest<?> request) {
        HttpClientResponseException e = assertThrows(
                HttpClientResponseException.class, () -> client.toBlocking().exchange(request));
        return e.getStatus();
    }

    @Test
    void signupIsNotPublic() {
        UserDto dto = new UserDto(
                null, "victim@example.com", "password123", true, null, null, List.of(), null, null, null, null);
        assertEquals(HttpStatus.UNAUTHORIZED, statusOf(HttpRequest.POST("/signup", dto)));
    }

    @Test
    void signupRejectsOrdinaryUsers() {
        UserDto dto = new UserDto(
                null, "victim@example.com", "password123", true, null, null, List.of(), null, null, null, null);
        assertEquals(
                HttpStatus.FORBIDDEN, statusOf(HttpRequest.POST("/signup", dto).bearerAuth(token("7", "ROLE_USER"))));
    }

    @Test
    void ownerCannotLinkAnIdentity() {
        assertEquals(
                HttpStatus.FORBIDDEN,
                statusOf(HttpRequest.POST("/v0/user/7/identities", LINK).bearerAuth(token("7", "ROLE_USER"))));
    }

    @Test
    void ownerCannotEraseTheAccountHere() {
        assertEquals(
                HttpStatus.FORBIDDEN, statusOf(HttpRequest.DELETE("/v0/user/7").bearerAuth(token("7", "ROLE_USER"))));
    }
}
