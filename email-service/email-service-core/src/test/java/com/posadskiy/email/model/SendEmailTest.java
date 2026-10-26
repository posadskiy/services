package com.posadskiy.email.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class SendEmailTest {

    @Test
    void testSendEmailRecord() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "Test Body");
        
        Assertions.assertEquals("user123", sendEmail.userId());
        Assertions.assertEquals("Test Subject", sendEmail.subject());
        Assertions.assertEquals("Test Body", sendEmail.body());
    }

    @Test
    void testSendEmailRecordEquality() {
        SendEmail email1 = new SendEmail("user123", "Test Subject", "Test Body");
        SendEmail email2 = new SendEmail("user123", "Test Subject", "Test Body");
        SendEmail email3 = new SendEmail("user456", "Test Subject", "Test Body");
        
        Assertions.assertEquals(email1, email2);
        Assertions.assertNotEquals(email1, email3);
        Assertions.assertEquals(email1.hashCode(), email2.hashCode());
        Assertions.assertNotEquals(email1.hashCode(), email3.hashCode());
    }

    @Test
    void testSendEmailToString() {
        SendEmail sendEmail = new SendEmail("user123", "Test Subject", "Test Body");
        String toString = sendEmail.toString();
        
        Assertions.assertTrue(toString.contains("user123"));
        Assertions.assertTrue(toString.contains("Test Subject"));
        Assertions.assertTrue(toString.contains("Test Body"));
    }
} 