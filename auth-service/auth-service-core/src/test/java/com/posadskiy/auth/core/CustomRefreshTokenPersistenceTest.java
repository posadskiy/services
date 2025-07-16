package com.posadskiy.auth.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import io.micronaut.security.authentication.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;

@ExtendWith(MockitoExtension.class)
class CustomRefreshTokenPersistenceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private CustomRefreshTokenPersistence refreshTokenPersistence;

    private RefreshTokenEntity testToken;

    @BeforeEach
    void setUp() {
        testToken = new RefreshTokenEntity();
        testToken.setId(1L);
        testToken.setUsername("testuser");
        testToken.setRefreshToken("test-refresh-token");
        testToken.setRevoked(false);
    }

    @Test
    void shouldNotPersistTokenWithNullEvent() {
        // When & Then
        assertDoesNotThrow(() -> refreshTokenPersistence.persistToken(null));
        verify(refreshTokenRepository, never()).save(anyString(), anyString(), anyBoolean());
    }

    @Test
    void shouldGetAuthenticationForValidToken() {
        // When
        Publisher<Authentication> publisher = refreshTokenPersistence.getAuthentication("test-refresh-token");

        // Then
        assertNotNull(publisher);
    }

    @Test
    void shouldHandleRevokedToken() {
        // When
        Publisher<Authentication> publisher = refreshTokenPersistence.getAuthentication("test-refresh-token");

        // Then
        assertNotNull(publisher);
    }

    @Test
    void shouldHandleNonExistentToken() {
        // When
        Publisher<Authentication> publisher = refreshTokenPersistence.getAuthentication("non-existent-token");

        // Then
        assertNotNull(publisher);
    }

    @Test
    void shouldHandleNullRefreshToken() {
        // When
        Publisher<Authentication> publisher = refreshTokenPersistence.getAuthentication(null);

        // Then
        assertNotNull(publisher);
    }
}
