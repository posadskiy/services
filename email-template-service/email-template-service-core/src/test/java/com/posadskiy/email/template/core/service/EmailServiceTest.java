package com.posadskiy.email.template.core.service;

import com.posadskiy.email.api.SendEmailForm;
import com.posadskiy.email.template.api.*;
import com.posadskiy.email.template.core.client.EmailOperations;
import com.posadskiy.email.template.core.client.UserClient;
import com.posadskiy.email.template.core.mapper.EmailFormMapper;
import com.posadskiy.user.api.UserDto;
import io.micronaut.core.io.Writable;
import io.micronaut.views.ViewsRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    // Use a real instance for EmailFormMapper
    private final EmailFormMapper emailFormMapper = new EmailFormMapper();
    @Mock
    private UserClient userClient;
    @Mock
    private EmailOperations emailOperations;

    @Mock
    private ViewsRenderer viewsRenderer;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(userClient, emailFormMapper, emailOperations, viewsRenderer);
    }

    @Test
    void testSendTemplatedEmail() throws IOException {
        // Given
        String authorization = "Bearer token";
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        Email email = new Email("Test Subject");
        Recipient recipient = new Recipient("user123");
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        UserDto userDto = new UserDto("user123", "john.doe", "john@example.com", "password");
        Writable writable = new Writable() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                outputStream.write("<html><body>Test email content</body></html>".getBytes());
            }

            @Override
            public void writeTo(Writer writer) throws IOException {
                writer.write("<html><body>Test email content</body></html>");
            }
        };

        // When
        when(userClient.getUserById(authorization, "user123")).thenReturn(userDto);
        when(viewsRenderer.render(eq(EmailService.EMAIL_TEMPLATE_PATH), any(), eq(null))).thenReturn(writable);

        // Then
        assertDoesNotThrow(() -> emailService.sendTemplatedEmail(authorization, emailFormDto));

        verify(userClient).getUserById(authorization, "user123");
        verify(viewsRenderer).render(eq(EmailService.EMAIL_TEMPLATE_PATH), any(), eq(null));
        verify(emailOperations).sendHtmlBasedEmail(eq(authorization), any(SendEmailForm.class));
    }

    @Test
    void testSendTemplatedEmailWithIOException() throws IOException {
        // Given
        String authorization = "Bearer token";
        Button button = new Button("Click me", "https://example.com");
        Content content = new Content("Welcome", "Header", "Body text", button);
        Email email = new Email("Test Subject");
        Recipient recipient = new Recipient("user123");
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        UserDto userDto = new UserDto("user123", "john.doe", "john@example.com", "password");
        Writable writable = new Writable() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                throw new IOException("Test exception");
            }

            @Override
            public void writeTo(Writer writer) throws IOException {
                throw new IOException("Test exception");
            }
        };

        // When
        when(userClient.getUserById(authorization, "user123")).thenReturn(userDto);
        when(viewsRenderer.render(eq(EmailService.EMAIL_TEMPLATE_PATH), any(), eq(null))).thenReturn(writable);

        // Then
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> emailService.sendTemplatedEmail(authorization, emailFormDto));
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("Test exception", exception.getCause().getMessage());
    }

    @Test
    void testSendTemplatedEmailWithNullValues() throws IOException {
        // Given
        String authorization = "Bearer token";
        Button button = new Button(null, null);
        Content content = new Content(null, null, null, button);
        Email email = new Email(null);
        Recipient recipient = new Recipient(null);
        EmailFormDto emailFormDto = new EmailFormDto(email, recipient, content);

        UserDto userDto = new UserDto(null, null, null, null);
        Writable writable = new Writable() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                outputStream.write("<html><body>Test email content</body></html>".getBytes());
            }

            @Override
            public void writeTo(Writer writer) throws IOException {
                writer.write("<html><body>Test email content</body></html>");
            }
        };

        // When
        when(userClient.getUserById(authorization, null)).thenReturn(userDto);
        when(viewsRenderer.render(eq(EmailService.EMAIL_TEMPLATE_PATH), any(), eq(null))).thenReturn(writable);

        // Then
        assertDoesNotThrow(() -> emailService.sendTemplatedEmail(authorization, emailFormDto));

        verify(userClient).getUserById(authorization, null);
        verify(viewsRenderer).render(eq(EmailService.EMAIL_TEMPLATE_PATH), any(), eq(null));
        verify(emailOperations).sendHtmlBasedEmail(eq(authorization), any(SendEmailForm.class));
    }
} 
