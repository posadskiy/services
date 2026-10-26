package com.posadskiy.email.template.core.mapper;

import com.posadskiy.email.template.api.*;
import com.posadskiy.email.template.core.model.EmailForm;
import com.posadskiy.user.api.UserDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailFormMapperTest {

    private final EmailFormMapper mapper = new EmailFormMapper();

    @Test
    void testToModel() {
        // Given
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        Email email = new Email("Test Subject");
        Recipient recipient = new Recipient("user123");
        EmailFormDto dto = new EmailFormDto(email, recipient, content);

        UserDto userDto = UserDto.legacy("user123", "john.doe", "john.doe@example.com", "password");

        // When
        EmailForm result = mapper.toModel(dto, userDto);

        // Then
        assertNotNull(result);
        assertEquals("Test Subject", result.email().subject());
        assertEquals("john.doe", result.recipient().name());
        assertEquals("john.doe@example.com", result.recipient().email());
        assertEquals("Welcome", result.content().name());
        assertEquals("Header", result.content().header());
        assertEquals("Body text", result.content().text());
        assertEquals("Click me", result.content().button().text());
        assertEquals("https://example.com", result.content().button().link());
    }

    @Test
    void testToModelWithNullValues() {
        // Given
        Button button = new Button(null, null);
        Content content = new Content(null, null, null, button);
        Email email = new Email(null);
        Recipient recipient = new Recipient(null);
        EmailFormDto dto = new EmailFormDto(email, recipient, content);

        UserDto userDto = UserDto.legacy(null, null, null, null);

        // When
        EmailForm result = mapper.toModel(dto, userDto);

        // Then
        assertNotNull(result);
        assertNull(result.email().subject());
        assertNull(result.recipient().name());
        assertNull(result.recipient().email());
        assertNull(result.content().name());
        assertNull(result.content().header());
        assertNull(result.content().text());
        assertNull(result.content().button().text());
        assertNull(result.content().button().link());
    }

    @Test
    void testToModelWithEmptyValues() {
        // Given
        Button button = new Button("", "");
        Content content = new Content("", "", "", button);
        Email email = new Email("");
        Recipient recipient = new Recipient("");
        EmailFormDto dto = new EmailFormDto(email, recipient, content);

        UserDto userDto = UserDto.legacy("", "", "", "");

        // When
        EmailForm result = mapper.toModel(dto, userDto);

        // Then
        assertNotNull(result);
        assertEquals("", result.email().subject());
        assertEquals("", result.recipient().name());
        assertEquals("", result.recipient().email());
        assertEquals("", result.content().name());
        assertEquals("", result.content().header());
        assertEquals("", result.content().text());
        assertEquals("", result.content().button().text());
        assertEquals("", result.content().button().link());
    }
} 
