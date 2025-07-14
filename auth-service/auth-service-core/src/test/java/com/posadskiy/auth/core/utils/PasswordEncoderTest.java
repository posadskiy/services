package com.posadskiy.auth.core.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    @Test
    void shouldEncodePassword() {
        // Given
        String rawPassword = "testPassword123";

        // When
        String encodedPassword = PasswordEncoder.encode(rawPassword);

        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$"));
    }

    @Test
    void shouldEncodeDifferentPasswordsDifferently() {
        // Given
        String password1 = "password1";
        String password2 = "password2";

        // When
        String encoded1 = PasswordEncoder.encode(password1);
        String encoded2 = PasswordEncoder.encode(password2);

        // Then
        assertNotEquals(encoded1, encoded2);
    }

    @Test
    void shouldEncodeSamePasswordDifferently() {
        // Given
        String password = "samePassword";

        // When
        String encoded1 = PasswordEncoder.encode(password);
        String encoded2 = PasswordEncoder.encode(password);

        // Then
        assertNotEquals(encoded1, encoded2); // BCrypt generates different salts
    }

    @Test
    void shouldHandleEmptyPassword() {
        // Given
        String emptyPassword = "";

        // When
        String encodedPassword = PasswordEncoder.encode(emptyPassword);

        // Then
        assertNotNull(encodedPassword);
        assertNotEquals(emptyPassword, encodedPassword);
    }

    @Test
    void shouldHandleNullPassword() {
        // Given
        String nullPassword = null;

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncoder.encode(nullPassword);
        });
    }

    @Test
    void shouldUseCorrectStrength() {
        // Given
        String password = "testPassword";

        // When
        String encodedPassword = PasswordEncoder.encode(password);

        // Then
        // BCrypt with strength 16 should produce a hash with 53 characters
        assertEquals(60, encodedPassword.length());
        assertTrue(encodedPassword.startsWith("$2a$16$"));
    }
} 