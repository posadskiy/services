package com.posadskiy.user.api;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class UserDtoTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testConstructionAndGetters() {
        UserDto dto = UserDto.legacy("id123", "username", "user@example.com", "password");
        assertEquals("id123", dto.id());
        assertEquals("username", dto.username());
        assertEquals("user@example.com", dto.email());
        assertEquals("password", dto.password());
    }

    @Test
    void testEqualsAndHashCode() {
        UserDto dto1 = UserDto.legacy("id", "user", "email", "pass");
        UserDto dto2 = UserDto.legacy("id", "user", "email", "pass");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testSerialization() throws Exception {
        UserDto dto = UserDto.legacy("id", "user", "email", "pass");
        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("user"));
        assertTrue(json.contains("email"));
        assertTrue(json.contains("id"));
        // Password should not be present in the JSON due to WRITE_ONLY
        assertFalse(json.contains("pass"));
        UserDto deserialized = objectMapper.readValue(json, UserDto.class);
        assertEquals("id", deserialized.id());
        assertEquals("user", deserialized.username());
        assertEquals("email", deserialized.email());
        assertNull(deserialized.password()); // password is not deserialized
    }
} 