package com.posadskiy.email.template.core.client;

public interface EmailOperations {
    void sendHtmlBasedEmail(String to, String subject, String body);
}
