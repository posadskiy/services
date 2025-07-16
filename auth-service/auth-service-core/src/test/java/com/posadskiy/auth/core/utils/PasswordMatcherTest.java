package com.posadskiy.auth.core.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordMatcherTest {

    @Test
    void shouldMatchValidPassword() {
        // Given
        String rawPassword = "testPassword123";
        String encodedPassword = PasswordEncoder.encode(rawPassword);

        // When
        boolean result = PasswordMatcher.match(rawPassword, encodedPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotMatchWrongPassword() {
        // Given
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        String encodedPassword = PasswordEncoder.encode(correctPassword);

        // When
        boolean result = PasswordMatcher.match(wrongPassword, encodedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldNotMatchEmptyPassword() {
        // Given
        String correctPassword = "correctPassword";
        String emptyPassword = "";
        String encodedPassword = PasswordEncoder.encode(correctPassword);

        // When
        boolean result = PasswordMatcher.match(emptyPassword, encodedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldHandleNullRawPassword() {
        // Given
        String correctPassword = "correctPassword";
        String encodedPassword = PasswordEncoder.encode(correctPassword);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordMatcher.match(null, encodedPassword);
        });
    }

    @Test
    void shouldHandleNullEncodedPassword() {
        // Given
        String rawPassword = "testPassword";

        // When
        boolean result = PasswordMatcher.match(rawPassword, null);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldMatchEmptyPasswordWithEmptyEncoded() {
        // Given
        String emptyPassword = "";
        String encodedEmptyPassword = PasswordEncoder.encode(emptyPassword);

        // When
        boolean result = PasswordMatcher.match(emptyPassword, encodedEmptyPassword);

        // Then
        assertTrue(result);
    }

    @Test
    void shouldNotMatchWithInvalidEncodedPassword() {
        // Given
        String rawPassword = "testPassword";
        String invalidEncodedPassword = "invalidHash";

        // When
        boolean result = PasswordMatcher.match(rawPassword, invalidEncodedPassword);

        // Then
        assertFalse(result);
    }

    @Test
    void shouldMatchMultipleEncodingsOfSamePassword() {
        // Given
        String rawPassword = "testPassword";
        String encoded1 = PasswordEncoder.encode(rawPassword);
        String encoded2 = PasswordEncoder.encode(rawPassword);

        // When
        boolean result1 = PasswordMatcher.match(rawPassword, encoded1);
        boolean result2 = PasswordMatcher.match(rawPassword, encoded2);

        // Then
        assertTrue(result1);
        assertTrue(result2);
        assertNotEquals(encoded1, encoded2); // Different salts
    }
}
