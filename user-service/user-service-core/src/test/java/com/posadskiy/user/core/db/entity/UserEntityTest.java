package com.posadskiy.user.core.db.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void testUserEntityCreation() {
        UserEntity user = new UserEntity();
        
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getUpdatedAt());
        assertNull(user.getCreatedAt());
    }
    
    @Test
    void testUserEntitySettersAndGetters() {
        UserEntity user = new UserEntity();
        LocalDateTime now = LocalDateTime.now();
        
        user.setId(123L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setUpdatedAt(now);
        user.setCreatedAt(now);
        
        assertEquals(123L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertEquals(now, user.getUpdatedAt());
        assertEquals(now, user.getCreatedAt());
    }
    
    @Test
    void testUserEntityWithNullValues() {
        UserEntity user = new UserEntity();
        
        user.setId(null);
        user.setUsername(null);
        user.setEmail(null);
        user.setPasswordHash(null);
        user.setUpdatedAt(null);
        user.setCreatedAt(null);
        
        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPasswordHash());
        assertNull(user.getUpdatedAt());
        assertNull(user.getCreatedAt());
    }
    
    @Test
    void testUserEntityEquality() {
        UserEntity user1 = new UserEntity();
        user1.setId(123L);
        user1.setUsername("testuser");
        user1.setEmail("test@example.com");
        user1.setPasswordHash("hashedPassword");
        
        UserEntity user2 = new UserEntity();
        user2.setId(123L);
        user2.setUsername("testuser");
        user2.setEmail("test@example.com");
        user2.setPasswordHash("hashedPassword");
        
        UserEntity user3 = new UserEntity();
        user3.setId(456L);
        user3.setUsername("testuser");
        user3.setEmail("test@example.com");
        user3.setPasswordHash("hashedPassword");
        
        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1.hashCode(), user3.hashCode());
    }
} 