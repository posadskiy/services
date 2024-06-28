package com.posadskiy.email.template.client;

public interface EmailOperations {
    void sendHtmlBasedEmail(String to, String subject, String body);
}
