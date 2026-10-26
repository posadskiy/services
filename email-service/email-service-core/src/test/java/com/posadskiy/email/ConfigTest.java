package com.posadskiy.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class ConfigTest {

    @Test
    void testConfigProperties() {
        Config config = new Config();
        
        // Test setters and getters
        config.setUsername("test@example.com");
        config.setPassword("testpassword");
        config.setProtocol("smtp");
        config.setHost("smtp.example.com");
        config.setPort("587");
        config.setAuth("true");
        config.setStartTlsEnable("true");
        config.setDebug("false");
        
        Assertions.assertEquals("test@example.com", config.getUsername());
        Assertions.assertEquals("testpassword", config.getPassword());
        Assertions.assertEquals("smtp", config.getProtocol());
        Assertions.assertEquals("smtp.example.com", config.getHost());
        Assertions.assertEquals("587", config.getPort());
        Assertions.assertEquals("true", config.getAuth());
        Assertions.assertEquals("true", config.getStartTlsEnable());
        Assertions.assertEquals("false", config.getDebug());
    }

    @Test
    void testConfigConstructor() {
        Config config = new Config();
        Assertions.assertNotNull(config);
    }
} 