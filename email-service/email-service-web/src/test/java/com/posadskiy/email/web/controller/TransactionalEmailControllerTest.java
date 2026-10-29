package com.posadskiy.email.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.posadskiy.email.api.TransactionalEmailRequest;
import com.posadskiy.email.resend.ResendTransactionalSender;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@MicronautTest
class TransactionalEmailControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenGenerator tokenGenerator;

    @MockBean
    @Replaces(ResendTransactionalSender.class)
    ResendTransactionalSender sender() {
        return mock(ResendTransactionalSender.class);
    }

    @Test
    void serviceTokenCanSend() {
        String token = tokenGenerator
                .generateToken(Authentication.build("auth-service", List.of("ROLE_SERVICE")), 60)
                .orElseThrow();
        var response = client.toBlocking()
                .exchange(HttpRequest.POST(
                                "/email/transactional",
                                new TransactionalEmailRequest(
                                        "Alpha <alpha@example.com>",
                                        "person@example.com",
                                        "verify-email",
                                        Map.of("VERIFY_URL", "https://grammarbar.com/verify-email?token=abc"),
                                        "verify-email/1/2",
                                        "verify_email"))
                        .bearerAuth(token));
        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
    }

    @Test
    void anonymousSendIsRejected() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.toBlocking()
                .exchange(HttpRequest.POST(
                        "/email/transactional",
                        new TransactionalEmailRequest(
                                "Alpha <alpha@example.com>",
                                "person@example.com",
                                "verify-email",
                                Map.of("VERIFY_URL", "https://grammarbar.com/verify-email?token=abc"),
                                "verify-email/1/2",
                                "verify_email"))));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }
}
