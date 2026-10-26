package com.posadskiy.email.template.api;

import io.micronaut.serde.ObjectMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class EmailFormDtoTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    void testConstructionAndGetters() {
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        Email email = new Email("Subject");
        Recipient recipient = new Recipient("user-id-123");
        EmailFormDto dto = new EmailFormDto(email, recipient, content);

        assertEquals(email, dto.email());
        assertEquals(recipient, dto.recipient());
        assertEquals(content, dto.content());
        assertEquals("Subject", dto.email().subject());
        assertEquals("user-id-123", dto.recipient().id());
        assertEquals("Welcome", dto.content().name());
        assertEquals("Header", dto.content().header());
        assertEquals("Body text", dto.content().text());
        assertEquals(button, dto.content().button());
        assertEquals("Click me", dto.content().button().text());
        assertEquals("https://example.com", dto.content().button().link());
    }

    @Test
    void testEqualsAndHashCode() {
        Button button1 = new Button("A", "B");
        Button button2 = new Button("A", "B");
        assertEquals(button1, button2);
        assertEquals(button1.hashCode(), button2.hashCode());

        Content content1 = new Content("N", "H", "T", button1);
        Content content2 = new Content("N", "H", "T", button2);
        assertEquals(content1, content2);
        assertEquals(content1.hashCode(), content2.hashCode());

        Email email1 = new Email("S");
        Email email2 = new Email("S");
        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());

        Recipient recipient1 = new Recipient("id");
        Recipient recipient2 = new Recipient("id");
        assertEquals(recipient1, recipient2);
        assertEquals(recipient1.hashCode(), recipient2.hashCode());

        EmailFormDto dto1 = new EmailFormDto(email1, recipient1, content1);
        EmailFormDto dto2 = new EmailFormDto(email2, recipient2, content2);
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testSerialization() throws Exception {
        Button button = new Button("Text", "Link");
        Content content = new Content("Name", "Header", "Text", button);
        Email email = new Email("Subject");
        Recipient recipient = new Recipient("id");
        EmailFormDto dto = new EmailFormDto(email, recipient, content);

        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("Subject"));
        assertTrue(json.contains("id"));
        assertTrue(json.contains("Name"));
        assertTrue(json.contains("Text"));
        assertTrue(json.contains("Link"));

        EmailFormDto deserialized = objectMapper.readValue(json, EmailFormDto.class);
        assertEquals(dto, deserialized);
    }
} 