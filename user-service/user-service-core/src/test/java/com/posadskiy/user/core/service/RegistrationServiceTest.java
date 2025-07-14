package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.UserEntity;
import com.posadskiy.user.core.exception.AuthException;
import com.posadskiy.user.core.mapper.entity.UserEntityMapper;
import com.posadskiy.user.core.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserEntityMapper userEntityMapper;

    @Mock
    private UsersRepository userRepository;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userEntityMapper, userRepository);
    }

    @Test
    void testRegistration_Success() {
        // Given
        User inputUser = new User(null, "testuser", "test@example.com", "password123");
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user123");
        userEntity.setUsername("testuser");
        userEntity.setEmail("test@example.com");
        userEntity.setPasswordHash("hashedPassword");

        User expectedUser = new User(1L, "testuser", "test@example.com", "hashedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userEntityMapper.mapToEntity(any())).thenReturn(userEntity);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userEntityMapper.mapFromEntity(userEntity)).thenReturn(expectedUser);

        // When
        User result = registrationService.registration(inputUser);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(userEntity);
        verify(userEntityMapper).mapFromEntity(userEntity);
    }

    @Test
    void testRegistration_WithBlankUsername() {
        // Given
        User inputUser = new User(null, "", "test@example.com", "password123");
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user123");
        userEntity.setUsername("test");
        userEntity.setEmail("test@example.com");
        userEntity.setPasswordHash("hashedPassword");

        User expectedUser = new User(1L, "test", "test@example.com", "hashedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userEntityMapper.mapToEntity(any())).thenReturn(userEntity);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userEntityMapper.mapFromEntity(userEntity)).thenReturn(expectedUser);

        // When
        User result = registrationService.registration(inputUser);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(userEntity);
        verify(userEntityMapper).mapFromEntity(userEntity);
    }

    @Test
    void testRegistration_WithNullUsername() {
        // Given
        User inputUser = new User(null, null, "test@example.com", "password123");
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user123");
        userEntity.setUsername("test");
        userEntity.setEmail("test@example.com");
        userEntity.setPasswordHash("hashedPassword");

        User expectedUser = new User(1L, "test", "test@example.com", "hashedPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userEntityMapper.mapToEntity(any())).thenReturn(userEntity);
        when(userRepository.save(any())).thenReturn(userEntity);
        when(userEntityMapper.mapFromEntity(userEntity)).thenReturn(expectedUser);

        // When
        User result = registrationService.registration(inputUser);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository).save(userEntity);
        verify(userEntityMapper).mapFromEntity(userEntity);
    }

    @Test
    void testRegistration_UserAlreadyExists() {
        // Given
        User inputUser = new User(null, "testuser", "test@example.com", "password123");
        UserEntity existingUser = new UserEntity();
        existingUser.setId("existing123");
        existingUser.setUsername("existinguser");
        existingUser.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        // When & Then
        AuthException exception = assertThrows(AuthException.class, () -> {
            registrationService.registration(inputUser);
        });

        assertEquals("User already exists", exception.getMessage());
        verify(userRepository).findByEmail("test@example.com");
        verify(userRepository, never()).save(any());
        verify(userEntityMapper, never()).mapFromEntity(any());
    }

    @Test
    void testRegistration_NullUser() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            registrationService.registration(null);
        });
    }
} 