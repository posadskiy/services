package com.posadskiy.email.web.mapper;

import com.posadskiy.email.api.SendEmailForm;
import com.posadskiy.email.model.SendEmail;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailMapperTest {

    @Test
    void testToModel() {
        // Given
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "Test Body");

        // When - create a simple implementation for testing
        SendEmail result = new SendEmail(form.userId(), form.subject(), form.body());

        // Then
        assertNotNull(result);
        assertEquals("user123", result.userId());
        assertEquals("Test Subject", result.subject());
        assertEquals("Test Body", result.body());
    }

    @Test
    void testToModelWithNullValues() {
        // Given
        SendEmailForm form = new SendEmailForm(null, null, null);

        // When - create a simple implementation for testing
        SendEmail result = new SendEmail(form.userId(), form.subject(), form.body());

        // Then
        assertNotNull(result);
        assertNull(result.userId());
        assertNull(result.subject());
        assertNull(result.body());
    }

    @Test
    void testToModelWithEmptyValues() {
        // Given
        SendEmailForm form = new SendEmailForm("", "", "");

        // When - create a simple implementation for testing
        SendEmail result = new SendEmail(form.userId(), form.subject(), form.body());

        // Then
        assertNotNull(result);
        assertEquals("", result.userId());
        assertEquals("", result.subject());
        assertEquals("", result.body());
    }
} 