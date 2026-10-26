package com.posadskiy.email.web.controller;

import com.posadskiy.email.api.SendEmailForm;
import com.posadskiy.email.service.EmailService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class EmailControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmailService emailService;

    @Test
    void testSendTextEmail_WithoutAuth() {
        // Given
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "Test Content");

        // When & Then - should fail due to missing authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/send/text", form)
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testSendHtmlEmail_WithoutAuth() {
        // Given
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "<html><body>Test Content</body></html>");

        // When & Then - should fail due to missing authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/send/html", form)
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testSendTextEmail_WithInvalidAuth() {
        // Given
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "Test Content");

        // When & Then - should fail due to invalid authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/send/text", form)
                    .header("Authorization", "Bearer invalid-token")
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testEmailServiceInjection() {
        // Test that EmailService is properly injected
        assertNotNull(emailService);
    }
} 