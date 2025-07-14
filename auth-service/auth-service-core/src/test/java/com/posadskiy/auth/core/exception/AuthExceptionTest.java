package com.posadskiy.auth.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthExceptionTest {

    @Test
    void shouldCreateAuthExceptionWithMessage() {
        // Given
        String message = "User authentication failed";

        // When
        AuthException exception = new AuthException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldCreateAuthExceptionWithEmptyMessage() {
        // Given
        String message = "";

        // When
        AuthException exception = new AuthException(message);

        // Then
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldCreateAuthExceptionWithNullMessage() {
        // Given
        String message = null;

        // When
        AuthException exception = new AuthException(message);

        // Then
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    void shouldBeInstanceOfRuntimeException() {
        // Given
        String message = "Test exception";

        // When
        AuthException exception = new AuthException(message);

        // Then
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void shouldHaveCorrectExceptionHierarchy() {
        // Given
        String message = "Test exception";

        // When
        AuthException exception = new AuthException(message);

        // Then
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
} 