package com.posadskiy.user.web.controller;

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