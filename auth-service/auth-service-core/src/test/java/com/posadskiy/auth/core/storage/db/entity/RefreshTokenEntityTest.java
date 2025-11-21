package com.posadskiy.auth.core.storage.db.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RefreshTokenEntityTest {

    @Test
    void shouldCreateRefreshTokenEntityWithValidData() {
        // Given
        Long id = 1L;
        String username = "testuser";
        String refreshToken = "refresh-token-123";
        Boolean revoked = false;
        LocalDateTime dateCreated = LocalDateTime.now();

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(id);
        token.setUsername(username);
        token.setRefreshToken(refreshToken);
        token.setRevoked(revoked);
        token.setDateCreated(dateCreated);

        // Then
        assertEquals(id, token.getId());
        assertEquals(username, token.getUsername());
        assertEquals(refreshToken, token.getRefreshToken());
        assertEquals(revoked, token.getRevoked());
        assertEquals(dateCreated, token.getDateCreated());
    }

    @Test
    void shouldCreateRefreshTokenEntityWithNullId() {
        // Given
        String username = "testuser";
        String refreshToken = "refresh-token-123";
        Boolean revoked = false;

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(null);
        token.setUsername(username);
        token.setRefreshToken(refreshToken);
        token.setRevoked(revoked);

        // Then
        assertNull(token.getId());
        assertEquals(username, token.getUsername());
        assertEquals(refreshToken, token.getRefreshToken());
        assertEquals(revoked, token.getRevoked());
    }

    @Test
    void shouldHandleRevokedToken() {
        // Given
        String username = "testuser";
        String refreshToken = "refresh-token-123";
        Boolean revoked = true;

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUsername(username);
        token.setRefreshToken(refreshToken);
        token.setRevoked(revoked);

        // Then
        assertEquals(username, token.getUsername());
        assertEquals(refreshToken, token.getRefreshToken());
        assertTrue(token.getRevoked());
    }

    @Test
    void shouldHandleEmptyStrings() {
        // Given
        String emptyUsername = "";
        String emptyRefreshToken = "";

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUsername(emptyUsername);
        token.setRefreshToken(emptyRefreshToken);

        // Then
        assertEquals(emptyUsername, token.getUsername());
        assertEquals(emptyRefreshToken, token.getRefreshToken());
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        String nullUsername = null;
        String nullRefreshToken = null;
        Boolean nullRevoked = null;
        LocalDateTime nullDateCreated = null;

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUsername(nullUsername);
        token.setRefreshToken(nullRefreshToken);
        token.setRevoked(nullRevoked);
        token.setDateCreated(nullDateCreated);

        // Then
        assertNull(token.getUsername());
        assertNull(token.getRefreshToken());
        assertNull(token.getRevoked());
        assertNull(token.getDateCreated());
    }

    @Test
    void shouldUpdateRefreshTokenEntity() {
        // Given
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setId(1L);
        token.setUsername("olduser");
        token.setRefreshToken("old-token");
        token.setRevoked(false);

        // When
        token.setUsername("newuser");
        token.setRefreshToken("new-token");
        token.setRevoked(true);

        // Then
        assertEquals("newuser", token.getUsername());
        assertEquals("new-token", token.getRefreshToken());
        assertTrue(token.getRevoked());
        assertEquals(1L, token.getId()); // ID should remain unchanged
    }

    @Test
    void shouldHandleSpecialCharactersInFields() {
        // Given
        String usernameWithSpecialChars = "user@123!#$";
        String refreshTokenWithSpecialChars = "refresh-token.with.special.chars!@#$%^&*()";

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUsername(usernameWithSpecialChars);
        token.setRefreshToken(refreshTokenWithSpecialChars);

        // Then
        assertEquals(usernameWithSpecialChars, token.getUsername());
        assertEquals(refreshTokenWithSpecialChars, token.getRefreshToken());
    }

    @Test
    void shouldHandleLongValues() {
        // Given
        String longUsername = "a".repeat(100);
        String longRefreshToken = "a".repeat(500);

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUsername(longUsername);
        token.setRefreshToken(longRefreshToken);

        // Then
        assertEquals(longUsername, token.getUsername());
        assertEquals(longRefreshToken, token.getRefreshToken());
    }

    @Test
    void shouldHandleDateTimeValues() {
        // Given
        LocalDateTime past = LocalDateTime.of(2020, 1, 1, 12, 0, 0);

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setDateCreated(past);

        // Then
        assertEquals(past, token.getDateCreated());
    }

    @Test
    void shouldHandleBooleanValues() {
        // Given
        Boolean trueValue = true;
        Boolean falseValue = false;

        // When
        RefreshTokenEntity token1 = new RefreshTokenEntity();
        token1.setRevoked(trueValue);

        RefreshTokenEntity token2 = new RefreshTokenEntity();
        token2.setRevoked(falseValue);

        // Then
        assertTrue(token1.getRevoked());
        assertFalse(token2.getRevoked());
    }

    @Test
    void shouldHandleJWTLikeTokens() {
        // Given
        String jwtLikeToken =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // When
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setRefreshToken(jwtLikeToken);

        // Then
        assertEquals(jwtLikeToken, token.getRefreshToken());
    }
}
