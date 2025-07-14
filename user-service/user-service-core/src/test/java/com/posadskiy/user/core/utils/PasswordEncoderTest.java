package com.posadskiy.user.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    @Test
    void testEncodePassword() {
        String rawPassword = "testPassword123";
        String encodedPassword = PasswordEncoder.encode(rawPassword);
        
        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$16$"));
    }
    
    @Test
    void testEncodeSamePasswordProducesDifferentHashes() {
        String rawPassword = "testPassword123";
        String encoded1 = PasswordEncoder.encode(rawPassword);
        String encoded2 = PasswordEncoder.encode(rawPassword);
        
        assertNotEquals(encoded1, encoded2);
    }
    
    @Test
    void testEncodeEmptyPassword() {
        String encodedPassword = PasswordEncoder.encode("");
        
        assertNotNull(encodedPassword);
        assertNotEquals("", encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$16$"));
    }
    
    @Test
    void testEncodeNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncoder.encode(null);
        });
    }
    
    @Test
    void testPasswordStrength() {
        assertEquals(16, PasswordEncoder.PASSWORD_STRENGTH);
    }
    
    @Test
    void testEncoderInstance() {
        assertNotNull(PasswordEncoder.encoder);
        assertTrue(PasswordEncoder.encoder instanceof org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder);
    }
} 