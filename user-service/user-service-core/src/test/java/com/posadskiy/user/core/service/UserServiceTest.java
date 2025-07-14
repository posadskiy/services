package com.posadskiy.user.core.service;

import com.posadskiy.user.core.db.UsersRepository;
import com.posadskiy.user.core.db.entity.UserEntity;
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
class UserServiceTest {

    @Mock
    private UserEntityMapper userEntityMapper;

    @Mock
    private UsersRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userEntityMapper, userRepository);
    }

    @Test
    void testGetUserById_Success() {
        // Given
        String userId = "user123";
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setUsername("testuser");
        userEntity.setEmail("test@example.com");
        userEntity.setPasswordHash("hashedPassword");

        User expectedUser = new User(1L, "testuser", "test@example.com", "hashedPassword");

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userEntityMapper.mapFromEntity(userEntity)).thenReturn(expectedUser);

        // When
        User result = userService.getUserById(userId);

        // Then
        assertNotNull(result);
        assertEquals(expectedUser, result);
        verify(userRepository).findById(userId);
        verify(userEntityMapper).mapFromEntity(userEntity);
    }

    @Test
    void testGetUserById_UserNotFound() {
        // Given
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(userId);
        });

        assertEquals("User with id " + userId + " not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(userEntityMapper, never()).mapFromEntity(any());
    }

    @Test
    void testGetUserById_NullId() {
        // Given
        when(userRepository.findById(null)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(null);
        });

        assertEquals("User with id null not found", exception.getMessage());
        verify(userRepository).findById(null);
        verify(userEntityMapper, never()).mapFromEntity(any());
    }
} 