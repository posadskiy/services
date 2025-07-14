package com.posadskiy.user.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testConfigProperties() {
        Config config = new Config();
        
        // Test default values
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertNull(config.getProtocol());
        assertNull(config.getHost());
        assertNull(config.getPort());
        assertNull(config.getAuth());
        assertNull(config.getStartTlsEnable());
        assertNull(config.getDebug());
        
        // Test setters
        config.setUsername("test@example.com");
        config.setPassword("password123");
        config.setProtocol("smtp");
        config.setHost("smtp.example.com");
        config.setPort("587");
        config.setAuth("true");
        config.setStartTlsEnable("true");
        config.setDebug("false");
        
        // Test getters
        assertEquals("test@example.com", config.getUsername());
        assertEquals("password123", config.getPassword());
        assertEquals("smtp", config.getProtocol());
        assertEquals("smtp.example.com", config.getHost());
        assertEquals("587", config.getPort());
        assertEquals("true", config.getAuth());
        assertEquals("true", config.getStartTlsEnable());
        assertEquals("false", config.getDebug());
    }
} 