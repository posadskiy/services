package com.posadskiy.auth.web;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.mail.TransactionalMail;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@Property(name = "auth.token.reuse-grace", value = "PT30S")
class RefreshReuseGraceTest extends PostgresMicronautTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    CapturingTransactionalMailer mailer;

    private TokenResponse signIn(String email) {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST("/signup", new RegisterRequest(email, "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        TransactionalMail mail = mailer.sent().getLast();
        String token = mail.variables().get("VERIFY_URL").substring("http://localhost:3000/verify-email?token=".length());
        client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token)));
        return client.toBlocking()
            .retrieve(HttpRequest.POST("/login", new LoginRequest(email, "correcthorse")), TokenResponse.class);
    }

    private TokenResponse refresh(String refreshToken) {
        return client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/oauth/token", new RefreshRequest("refresh_token", refreshToken, null, null, null, null)),
                TokenResponse.class);
    }

    @Test
    void aTokenRotatedASecondAgoIsStillAnswered() {
        TokenResponse first = signIn("grace@example.com");

        TokenResponse winner = refresh(first.refreshToken());
        TokenResponse retry = refresh(first.refreshToken());

        assertNotNull(retry.accessToken());
        assertNotEquals(winner.refreshToken(), retry.refreshToken());
        // Both successors belong to the same family and stay usable.
        assertNotNull(refresh(winner.refreshToken()).accessToken());
        assertNotNull(refresh(retry.refreshToken()).accessToken());
    }

    @Test
    void reuseAfterTheReplacementWasUsedRevokesTheFamily() {
        TokenResponse first = signIn("theft@example.com");

        TokenResponse second = refresh(first.refreshToken());
        refresh(second.refreshToken());

        HttpClientResponseException reuse =
            assertThrows(HttpClientResponseException.class, () -> refresh(first.refreshToken()));
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.getStatus());
    }
}
