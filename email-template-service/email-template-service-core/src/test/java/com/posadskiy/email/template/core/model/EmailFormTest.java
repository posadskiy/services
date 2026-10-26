package com.posadskiy.email.template.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailFormTest {

    @Test
    void testConstructionAndGetters() {
        // Given
        Email email = new Email("Test Subject");
        Recipient recipient = new Recipient("John Doe", "john@example.com");
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        EmailForm emailForm = new EmailForm(email, recipient, content);

        // Then
        assertEquals(email, emailForm.email());
        assertEquals(recipient, emailForm.recipient());
        assertEquals(content, emailForm.content());
        assertEquals("Test Subject", emailForm.email().subject());
        assertEquals("John Doe", emailForm.recipient().name());
        assertEquals("john@example.com", emailForm.recipient().email());
        assertEquals("Welcome", emailForm.content().name());
        assertEquals("Header", emailForm.content().header());
        assertEquals("Body text", emailForm.content().text());
        assertEquals("Click me", emailForm.content().button().text());
        assertEquals("https://example.com", emailForm.content().button().link());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        Email email1 = new Email("Subject");
        Email email2 = new Email("Subject");
        Recipient recipient1 = new Recipient("Name", "email@example.com");
        Recipient recipient2 = new Recipient("Name", "email@example.com");
        Button button1 = new Button("Text", "Link");
        Button button2 = new Button("Text", "Link");
        Content content1 = new Content("Name", "Header", "Text", button1);
        Content content2 = new Content("Name", "Header", "Text", button2);
        EmailForm form1 = new EmailForm(email1, recipient1, content1);
        EmailForm form2 = new EmailForm(email2, recipient2, content2);

        // Then
        assertEquals(form1, form2);
        assertEquals(form1.hashCode(), form2.hashCode());
    }

    @Test
    void testWithNullValues() {
        // Given
        Email email = new Email(null);
        Recipient recipient = new Recipient(null, null);
        Button button = new Button(null, null);
        Content content = new Content(null, null, null, button);
        EmailForm emailForm = new EmailForm(email, recipient, content);

        // Then
        assertNotNull(emailForm);
        assertNull(emailForm.email().subject());
        assertNull(emailForm.recipient().name());
        assertNull(emailForm.recipient().email());
        assertNull(emailForm.content().name());
        assertNull(emailForm.content().header());
        assertNull(emailForm.content().text());
        assertNull(emailForm.content().button().text());
        assertNull(emailForm.content().button().link());
    }
} 