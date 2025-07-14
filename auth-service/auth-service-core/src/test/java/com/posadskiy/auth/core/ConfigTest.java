package com.posadskiy.auth.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
    }

    @Test
    void shouldCreateConfigInstance() {
        // Given & When
        Config newConfig = new Config();

        // Then
        assertNotNull(newConfig);
    }

    @Test
    void shouldAllowSettingConfigurationValues() {
        // Given
        String testUsername = "test@example.com";
        String testPassword = "testpassword";
        String testProtocol = "smtp";
        String testHost = "smtp.example.com";
        String testPort = "587";
        String testAuth = "true";
        String testStartTlsEnable = "true";
        String testDebug = "false";

        // When
        config.setUsername(testUsername);
        config.setPassword(testPassword);
        config.setProtocol(testProtocol);
        config.setHost(testHost);
        config.setPort(testPort);
        config.setAuth(testAuth);
        config.setStartTlsEnable(testStartTlsEnable);
        config.setDebug(testDebug);

        // Then
        assertEquals(testUsername, config.getUsername());
        assertEquals(testPassword, config.getPassword());
        assertEquals(testProtocol, config.getProtocol());
        assertEquals(testHost, config.getHost());
        assertEquals(testPort, config.getPort());
        assertEquals(testAuth, config.getAuth());
        assertEquals(testStartTlsEnable, config.getStartTlsEnable());
        assertEquals(testDebug, config.getDebug());
    }

    @Test
    void shouldHandleNullValues() {
        // Given
        String nullValue = null;

        // When
        config.setUsername(nullValue);
        config.setPassword(nullValue);
        config.setProtocol(nullValue);
        config.setHost(nullValue);
        config.setPort(nullValue);
        config.setAuth(nullValue);
        config.setStartTlsEnable(nullValue);
        config.setDebug(nullValue);

        // Then
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertNull(config.getProtocol());
        assertNull(config.getHost());
        assertNull(config.getPort());
        assertNull(config.getAuth());
        assertNull(config.getStartTlsEnable());
        assertNull(config.getDebug());
    }

    @Test
    void shouldHandleEmptyStrings() {
        // Given
        String emptyValue = "";

        // When
        config.setUsername(emptyValue);
        config.setPassword(emptyValue);
        config.setProtocol(emptyValue);
        config.setHost(emptyValue);
        config.setPort(emptyValue);
        config.setAuth(emptyValue);
        config.setStartTlsEnable(emptyValue);
        config.setDebug(emptyValue);

        // Then
        assertEquals(emptyValue, config.getUsername());
        assertEquals(emptyValue, config.getPassword());
        assertEquals(emptyValue, config.getProtocol());
        assertEquals(emptyValue, config.getHost());
        assertEquals(emptyValue, config.getPort());
        assertEquals(emptyValue, config.getAuth());
        assertEquals(emptyValue, config.getStartTlsEnable());
        assertEquals(emptyValue, config.getDebug());
    }

    @Test
    void shouldHandleSpecialCharacters() {
        // Given
        String specialValue = "test@example.com!@#$%^&*()";

        // When
        config.setUsername(specialValue);
        config.setPassword(specialValue);
        config.setProtocol(specialValue);
        config.setHost(specialValue);
        config.setPort(specialValue);
        config.setAuth(specialValue);
        config.setStartTlsEnable(specialValue);
        config.setDebug(specialValue);

        // Then
        assertEquals(specialValue, config.getUsername());
        assertEquals(specialValue, config.getPassword());
        assertEquals(specialValue, config.getProtocol());
        assertEquals(specialValue, config.getHost());
        assertEquals(specialValue, config.getPort());
        assertEquals(specialValue, config.getAuth());
        assertEquals(specialValue, config.getStartTlsEnable());
        assertEquals(specialValue, config.getDebug());
    }

    @Test
    void shouldHandleLongValues() {
        // Given
        String longValue = "a".repeat(1000);

        // When
        config.setUsername(longValue);
        config.setPassword(longValue);
        config.setProtocol(longValue);
        config.setHost(longValue);
        config.setPort(longValue);
        config.setAuth(longValue);
        config.setStartTlsEnable(longValue);
        config.setDebug(longValue);

        // Then
        assertEquals(longValue, config.getUsername());
        assertEquals(longValue, config.getPassword());
        assertEquals(longValue, config.getProtocol());
        assertEquals(longValue, config.getHost());
        assertEquals(longValue, config.getPort());
        assertEquals(longValue, config.getAuth());
        assertEquals(longValue, config.getStartTlsEnable());
        assertEquals(longValue, config.getDebug());
    }
} 