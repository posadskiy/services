package com.posadskiy.auth.core.storage.db.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserEntityTest {

    @Test
    void shouldCreateUserEntityWithValidData() {
        // Given
        Long id = 1L;
        String username = "testuser";
        String email = "test@example.com";
        String passwordHash = "hashedPassword123";
        LocalDateTime now = LocalDateTime.now();

        // When
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // Then
        assertEquals(id, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
    }

    @Test
    void shouldCreateUserEntityWithNullId() {
        // Given
        String username = "testuser";
        String email = "test@example.com";
        String passwordHash = "hashedPassword123";

        // When
        UserEntity user = new UserEntity();
        user.setId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);

        // Then
        assertNull(user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(email, user.getEmail());
        assertEquals(passwordHash, user.getPasswordHash());
    }

    @Test
    void shouldHandleEmptyStrings() {
        // Given
        String emptyUsername = "";
        String emptyEmail = "";
        String emptyPasswordHash = "";

        // When
        UserEntity user = new UserEntity();
        user.setUsername(emptyUsername);
        user.setEmail(emptyEmail);
        user.setPasswordHash(emptyPasswordHash);

        // Then
        assertEquals(emptyUsername, user.getUsername());
        assertEquals(emptyEmail, user.getEmail());
        assertEquals(emptyPasswordHash, user.getPasswordHash());
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        String nullUsername = null;
        String nullEmail = null;
        String nullPasswordHash = null;
        LocalDateTime nullDateTime = null;

        // When
        UserEntity user = new UserEntity();
        user.setUsername(nullUsername);
        user.setEmail(nullEmail);
        user.setPasswordHash(nullPasswordHash);
        user.setCreatedAt(nullDateTime);
        user.setUpdatedAt(nullDateTime);

        // Then
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getCreatedAt());
        assertNull(user.getUpdatedAt());
    }

    @Test
    void shouldUpdateUserEntity() {
        // Given
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("olduser");
        user.setEmail("old@example.com");
        user.setPasswordHash("oldhash");

        // When
        user.setUsername("newuser");
        user.setEmail("new@example.com");
        user.setPasswordHash("newhash");

        // Then
        assertEquals("newuser", user.getUsername());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("newhash", user.getPasswordHash());
        assertEquals(1L, user.getId()); // ID should remain unchanged
    }

    @Test
    void shouldHandleSpecialCharactersInFields() {
        // Given
        String usernameWithSpecialChars = "user@123!#$";
        String emailWithSpecialChars = "test+user@example-domain.com";
        String passwordHashWithSpecialChars = "$2a$16$hash.with.special.chars!@#";

        // When
        UserEntity user = new UserEntity();
        user.setUsername(usernameWithSpecialChars);
        user.setEmail(emailWithSpecialChars);
        user.setPasswordHash(passwordHashWithSpecialChars);

        // Then
        assertEquals(usernameWithSpecialChars, user.getUsername());
        assertEquals(emailWithSpecialChars, user.getEmail());
        assertEquals(passwordHashWithSpecialChars, user.getPasswordHash());
    }

    @Test
    void shouldHandleLongValues() {
        // Given
        String longUsername = "a".repeat(100);
        String longEmail = "a".repeat(50) + "@" + "b".repeat(50) + ".com";
        String longPasswordHash = "a".repeat(200);

        // When
        UserEntity user = new UserEntity();
        user.setUsername(longUsername);
        user.setEmail(longEmail);
        user.setPasswordHash(longPasswordHash);

        // Then
        assertEquals(longUsername, user.getUsername());
        assertEquals(longEmail, user.getEmail());
        assertEquals(longPasswordHash, user.getPasswordHash());
    }

    @Test
    void shouldHandleDateTimeValues() {
        // Given
        LocalDateTime past = LocalDateTime.of(2020, 1, 1, 12, 0, 0);
        LocalDateTime future = LocalDateTime.of(2030, 12, 31, 23, 59, 59);

        // When
        UserEntity user = new UserEntity();
        user.setCreatedAt(past);
        user.setUpdatedAt(future);

        // Then
        assertEquals(past, user.getCreatedAt());
        assertEquals(future, user.getUpdatedAt());
    }
}
