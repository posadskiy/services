package com.posadskiy.email.web;

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
class EmailIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmailService emailService;

    @Test
    void testApplicationStartup() {
        // Test that the application starts successfully
        assertNotNull(client);
        assertNotNull(emailService);
    }

    @Test
    void testSendTextEmail_WithoutAuth() {
        // Given
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "Test Body");

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
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "<html><body>Test Body</body></html>");

        // When & Then - should fail due to missing authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/send/html", form)
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testEmailServiceProperties() {
        // Test that EmailService has the required properties
        assertNotNull(emailService);
        
        // Test that properties can be retrieved (this will use the test configuration)
        var properties = emailService.getProperties();
        assertNotNull(properties);
        assertNotNull(properties.getProperty("mail.smtp.host"));
        assertNotNull(properties.getProperty("mail.smtp.port"));
    }
} 