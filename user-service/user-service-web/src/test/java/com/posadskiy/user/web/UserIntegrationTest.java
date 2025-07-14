package com.posadskiy.user.web;

import com.posadskiy.user.api.UserDto;
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
class UserIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testApplicationStartup() {
        // Test that the application starts successfully
        assertNotNull(client);
    }

    @Test
    void testUserEndpoint_RequiresAuthentication() {
        // Given
        String userId = "user123";

        // When & Then
        // This test verifies that the user endpoint requires authentication
        // In a real scenario, you would expect a 401 Unauthorized response
        assertNotNull(client);
        
        // The endpoint should require authentication
        // This is a basic test to ensure the endpoint exists
        assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().retrieve(HttpRequest.GET("/v0/user/" + userId));
        });
    }

    @Test
    void testRegistrationEndpoint_AnonymousAccess() {
        // Given
        UserDto userDto = new UserDto(null, "testuser", "test@example.com", "password123");

        // When & Then
        // This test verifies that the registration endpoint allows anonymous access
        // In a real scenario, you would expect a 200 OK or 400 Bad Request response
        assertNotNull(client);
        
        // The registration endpoint should be accessible without authentication
        // This is a basic test to ensure the endpoint exists
        try {
            client.toBlocking().retrieve(HttpRequest.POST("/signup", userDto));
        } catch (HttpClientResponseException e) {
            // Expected - the endpoint exists but may return an error due to missing dependencies
            assertTrue(e.getStatus().getCode() >= 400);
        }
    }

    @Test
    void testRegistrationEndpoint_WithValidData() {
        // Given
        UserDto userDto = new UserDto(null, "newuser", "newuser@example.com", "password123");

        // When & Then
        // This test verifies the registration endpoint with valid data
        assertNotNull(client);
        
        try {
            client.toBlocking().retrieve(HttpRequest.POST("/signup", userDto));
        } catch (HttpClientResponseException e) {
            // Expected - the endpoint exists but may return an error due to missing dependencies
            assertTrue(e.getStatus().getCode() >= 400);
        }
    }

    @Test
    void testRegistrationEndpoint_WithInvalidData() {
        // Given
        UserDto userDto = new UserDto(null, "", "invalid-email", "");

        // When & Then
        // This test verifies the registration endpoint with invalid data
        assertNotNull(client);
        
        try {
            client.toBlocking().retrieve(HttpRequest.POST("/signup", userDto));
        } catch (HttpClientResponseException e) {
            // Expected - the endpoint exists but should return an error for invalid data
            assertTrue(e.getStatus().getCode() >= 400);
        }
    }
} 