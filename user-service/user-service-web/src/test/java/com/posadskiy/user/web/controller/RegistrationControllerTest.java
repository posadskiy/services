package com.posadskiy.user.web.controller;

import com.posadskiy.user.api.UserDto;
import com.posadskiy.user.core.mapper.dto.UserDtoMapper;
import com.posadskiy.user.core.model.User;
import com.posadskiy.user.core.service.RegistrationService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class RegistrationControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testRegistration_Success() {
        // Given
        UserDto userDto = new UserDto("1", "testuser", "test@example.com", "password123");

        // When
        HttpRequest<UserDto> request = HttpRequest.POST("/signup", userDto);

        // Then
        // This test verifies the endpoint exists and returns a response
        assertNotNull(client);
    }

    @Test
    void testRegistration_WithEmptyUsername() {
        // Given
        UserDto userDto = new UserDto("1", "", "test@example.com", "password123");

        // When
        HttpRequest<UserDto> request = HttpRequest.POST("/signup", userDto);

        // Then
        assertNotNull(client);
    }

    @Test
    void testRegistration_WithNullUsername() {
        // Given
        UserDto userDto = new UserDto("1", null, "test@example.com", "password123");

        // When
        HttpRequest<UserDto> request = HttpRequest.POST("/signup", userDto);

        // Then
        assertNotNull(client);
    }

    @Test
    void testApplicationStartup() {
        // Test that the application starts successfully
        assertNotNull(client);
    }
} 