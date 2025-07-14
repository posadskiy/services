package com.posadskiy.email.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class SendEmailFormTest {

    @Test
    void testSendEmailFormRecord() {
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "Test Body");
        
        Assertions.assertEquals("user123", form.userId());
        Assertions.assertEquals("Test Subject", form.subject());
        Assertions.assertEquals("Test Body", form.body());
    }

    @Test
    void testSendEmailFormRecordEquality() {
        SendEmailForm form1 = new SendEmailForm("user123", "Test Subject", "Test Body");
        SendEmailForm form2 = new SendEmailForm("user123", "Test Subject", "Test Body");
        SendEmailForm form3 = new SendEmailForm("user456", "Test Subject", "Test Body");
        
        Assertions.assertEquals(form1, form2);
        Assertions.assertNotEquals(form1, form3);
        Assertions.assertEquals(form1.hashCode(), form2.hashCode());
        Assertions.assertNotEquals(form1.hashCode(), form3.hashCode());
    }

    @Test
    void testSendEmailFormToString() {
        SendEmailForm form = new SendEmailForm("user123", "Test Subject", "Test Body");
        String toString = form.toString();
        
        Assertions.assertTrue(toString.contains("user123"));
        Assertions.assertTrue(toString.contains("Test Subject"));
        Assertions.assertTrue(toString.contains("Test Body"));
    }

    @Test
    void testSendEmailFormWithNullValues() {
        SendEmailForm form = new SendEmailForm(null, null, null);
        
        Assertions.assertNull(form.userId());
        Assertions.assertNull(form.subject());
        Assertions.assertNull(form.body());
    }
} 