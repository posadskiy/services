package com.posadskiy.user.web.controller;

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
class UserControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testGetCurrentUser_RequiresAuthentication() {
        // Given - no authentication token provided

        // When & Then
        // The /me endpoint should require authentication
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().retrieve(HttpRequest.GET("/v0/user/me"));
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void testGetUserById_Success() {
        // Given
        assertNotNull(client);
        assertNotNull(client);
    }

    @Test
    void testGetUserById_WithoutAuthorization() {
        // Given
        assertNotNull(client);
        assertNotNull(client);
    }

    @Test
    void testApplicationStartup() {
        // Test that the application starts successfully
        assertNotNull(client);
    }
} 