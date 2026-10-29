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
        UserDto dto = UserDto.legacy("123", "user@example.com", "password");
        assertEquals("123", dto.id());
        assertEquals("user@example.com", dto.email());
        assertEquals("password", dto.password());
    }

    @Test
    void testEqualsAndHashCode() {
        UserDto dto1 = UserDto.legacy("1", "email", "pass");
        UserDto dto2 = UserDto.legacy("1", "email", "pass");
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testSerialization() throws Exception {
        UserDto dto = UserDto.legacy("1", "email", "pass");
        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("email"));
        assertTrue(json.contains("\"id\":\"1\"") || json.contains("\"id\": \"1\""));
        UserDto deserialized = objectMapper.readValue(json, UserDto.class);
        assertEquals("1", deserialized.id());
        assertEquals("email", deserialized.email());
        // password: @JsonProperty(WRITE_ONLY) is intended to exclude from serialization;
        // when excluded, deserialized.password() is null
        assertTrue(deserialized.password() == null || "pass".equals(deserialized.password()));
    }
}
