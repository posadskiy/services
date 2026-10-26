package com.posadskiy.email.service;

import com.posadskiy.email.Config;
import com.posadskiy.email.client.UserClient;
import com.posadskiy.email.enums.ContentType;
import com.posadskiy.email.model.SendEmail;
import com.posadskiy.user.api.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Properties;

class EmailServiceTest {

    private Config config;
    private UserClient userClient;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setHost("smtp.example.com");
        config.setPort("587");
        config.setAuth("true");
        config.setStartTlsEnable("true");
        config.setDebug("false");
        config.setUsername("test@example.com");
        config.setPassword("testpassword");
        
        // Create a simple mock using anonymous class
        userClient = new UserClient() {
            @Override
            public UserDto getUserById(String authorization, String id) {
                return UserDto.legacy("user123", "testuser", "test@example.com", "password");
            }
        };
        
        emailService = new EmailService(config, userClient);
    }

    @Test
    void testGetProperties() {
        Properties properties = emailService.getProperties();
        
        Assertions.assertEquals("smtp.example.com", properties.getProperty("mail.smtp.host"));
        Assertions.assertEquals("587", properties.getProperty("mail.smtp.port"));
        Assertions.assertEquals("true", properties.getProperty("mail.smtp.auth"));
        Assertions.assertEquals("true", properties.getProperty("mail.smtp.starttls.enable"));
        Assertions.assertEquals("false", properties.getProperty("mail.smtps.debug"));
    }

    @Test
    void testSendText() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "Test Body");
        
        // This will likely fail due to SMTP setup, but we can test the method structure
        Assertions.assertDoesNotThrow(() -> {
            emailService.sendText("Bearer token", sendEmail);
        });
    }

    @Test
    void testSendHtml() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "<html><body>Test Body</body></html>");
        
        Assertions.assertDoesNotThrow(() -> {
            emailService.sendHtml("Bearer token", sendEmail);
        });
    }

    @Test
    void testSendEmailWithTextContentType() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "Test Body");
        
        Assertions.assertDoesNotThrow(() -> {
            emailService.sendEmail("Bearer token", sendEmail, ContentType.Text);
        });
    }

    @Test
    void testSendEmailWithHtmlContentType() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "<html><body>Test Body</body></html>");
        
        Assertions.assertDoesNotThrow(() -> {
            emailService.sendEmail("Bearer token", sendEmail, ContentType.HTML);
        });
    }

    @Test
    void testSendEmailWithNullUser() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "Test Body");
        
        // Create a mock that returns null
        UserClient nullUserClient = new UserClient() {
            @Override
            public UserDto getUserById(String authorization, String id) {
                return null;
            }
        };
        
        EmailService emailServiceWithNullUser = new EmailService(config, nullUserClient);
        
        // Expect NullPointerException when user is null
        Assertions.assertThrows(NullPointerException.class, () -> {
            emailServiceWithNullUser.sendEmail("Bearer token", sendEmail, ContentType.Text);
        });
    }
} 