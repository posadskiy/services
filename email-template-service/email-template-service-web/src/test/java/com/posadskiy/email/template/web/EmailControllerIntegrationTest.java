package com.posadskiy.email.template.web;

import com.posadskiy.email.template.api.Button;
import com.posadskiy.email.template.api.Content;
import com.posadskiy.email.template.api.Email;
import com.posadskiy.email.template.api.EmailFormDto;
import com.posadskiy.email.template.api.Recipient;
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
class EmailControllerIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testSendBaseTemplatedEmail_WithoutAuth() {
        // Given
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        Email email = new Email("Test Subject");
        Recipient recipient = new Recipient("user123");
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        // When & Then - should fail due to missing authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/template/send/base", emailFormDto)
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testSendBaseTemplatedEmail_WithInvalidAuth() {
        // Given
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        Email email = new Email("Test Subject");
        Recipient recipient = new Recipient("user123");
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        // When & Then - should fail due to invalid authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/template/send/base", emailFormDto)
                    .header("Authorization", "Bearer invalid-token")
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testSendBaseTemplatedEmail_WithNullValues() {
        // Given
        Button button = new Button(null, null);
        Content content = new Content(null, null, null, button);
        Email email = new Email(null);
        Recipient recipient = new Recipient(null);
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        // When & Then - should fail due to missing authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/template/send/base", emailFormDto)
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void testSendBaseTemplatedEmail_WithEmptyValues() {
        // Given
        Button button = new Button("", "");
        Content content = new Content("", "", "", button);
        Email email = new Email("");
        Recipient recipient = new Recipient("");
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        // When & Then - should fail due to missing authentication
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/email/template/send/base", emailFormDto)
            );
        });
        
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }
} 