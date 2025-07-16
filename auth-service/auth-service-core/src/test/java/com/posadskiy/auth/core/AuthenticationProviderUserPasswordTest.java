package com.posadskiy.auth.core;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import com.posadskiy.auth.core.utils.PasswordEncoder;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationProviderUserPasswordTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private HttpRequest<Object> httpRequest;

    @InjectMocks
    private AuthenticationProviderUserPassword<Object> authenticationProvider;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        // Use a real BCrypt hash for "correctpassword"
        testUser.setPasswordHash(PasswordEncoder.encode("correctpassword"));
    }

    @Test
    void shouldAuthenticateSystemUser() {
        // Given
        UsernamePasswordCredentials authRequest = new UsernamePasswordCredentials("system", "anypassword");

        // When
        AuthenticationResponse response = authenticationProvider.authenticate(httpRequest, authRequest);

        // Then
        assertTrue(response.isAuthenticated());
        assertEquals("system", response.getAuthentication().get().getName());
    }

    @Test
    void shouldReturnFailureWhenUserNotFound() {
        // Given
        when(usersRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        UsernamePasswordCredentials authRequest =
                new UsernamePasswordCredentials("nonexistent@example.com", "password");

        // When
        AuthenticationResponse response = authenticationProvider.authenticate(httpRequest, authRequest);

        // Then
        assertFalse(response.isAuthenticated());
    }

    @Test
    void shouldReturnFailureWhenPasswordDoesNotMatch() {
        // Given
        when(usersRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UsernamePasswordCredentials authRequest = new UsernamePasswordCredentials("test@example.com", "wrongpassword");

        // When
        AuthenticationResponse response = authenticationProvider.authenticate(httpRequest, authRequest);

        // Then
        assertFalse(response.isAuthenticated());
    }

    @Test
    void shouldReturnSuccessWhenCredentialsMatch() {
        // Given
        when(usersRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UsernamePasswordCredentials authRequest =
                new UsernamePasswordCredentials("test@example.com", "correctpassword");

        // When
        AuthenticationResponse response = authenticationProvider.authenticate(httpRequest, authRequest);

        // Then
        assertTrue(response.isAuthenticated());
        assertEquals("1", response.getAuthentication().get().getName());
    }

    @Test
    void shouldHandleNullHttpRequest() {
        // Given
        when(usersRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UsernamePasswordCredentials authRequest =
                new UsernamePasswordCredentials("test@example.com", "correctpassword");

        // When
        AuthenticationResponse response = authenticationProvider.authenticate(null, authRequest);

        // Then
        assertTrue(response.isAuthenticated());
        assertEquals("1", response.getAuthentication().get().getName());
    }

    @Test
    void shouldHandleEmptyPassword() {
        // Given
        when(usersRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UsernamePasswordCredentials authRequest = new UsernamePasswordCredentials("test@example.com", "");

        // When
        AuthenticationResponse response = authenticationProvider.authenticate(httpRequest, authRequest);

        // Then
        assertFalse(response.isAuthenticated());
    }

    @Test
    void shouldHandleNullPassword() {
        // Given
        when(usersRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UsernamePasswordCredentials authRequest = new UsernamePasswordCredentials("test@example.com", null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationProvider.authenticate(httpRequest, authRequest);
        });
    }
}
