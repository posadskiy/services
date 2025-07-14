package com.posadskiy.user.core.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserRecord() {
        User user = new User(1L, "testuser", "test@example.com", "password123");
        
        assertEquals(1L, user.id());
        assertEquals("testuser", user.username());
        assertEquals("test@example.com", user.email());
        assertEquals("password123", user.password());
    }
    
    @Test
    void testUserRecordWithNullValues() {
        User user = new User(null, null, null, null);
        
        assertNull(user.id());
        assertNull(user.username());
        assertNull(user.email());
        assertNull(user.password());
    }
    
    @Test
    void testUserRecordEquality() {
        User user1 = new User(1L, "testuser", "test@example.com", "password123");
        User user2 = new User(1L, "testuser", "test@example.com", "password123");
        User user3 = new User(2L, "testuser", "test@example.com", "password123");
        
        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1.hashCode(), user3.hashCode());
    }
    
    @Test
    void testUserRecordToString() {
        User user = new User(1L, "testuser", "test@example.com", "password123");
        String toString = user.toString();
        
        assertTrue(toString.contains("User"));
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("username=testuser"));
        assertTrue(toString.contains("email=test@example.com"));
        assertTrue(toString.contains("password=password123"));
    }
} 