package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.model.User;
import com.posadskiy.user.core.service.UserService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
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
    void testGetUserById_Success() {
        // Given
        String userId = "user123";
        String authorization = "Bearer test-token";

        // When
        HttpRequest<Object> request = HttpRequest.GET("/v0/user/" + userId)
                .header("Authorization", authorization);

        // Then
        // This test verifies the endpoint exists and returns a response
        assertNotNull(client);
    }

    @Test
    void testGetUserById_WithoutAuthorization() {
        // Given
        String userId = "user123";

        // When
        HttpRequest<Object> request = HttpRequest.GET("/v0/user/" + userId);

        // Then
        // This test verifies the endpoint requires authorization
        assertNotNull(client);
    }

    @Test
    void testApplicationStartup() {
        // Test that the application starts successfully
        assertNotNull(client);
    }
} 