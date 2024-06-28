package com.posadskiy.email.template.service;

import com.posadskiy.email.template.client.EmailOperations;
import com.posadskiy.email.template.form.EmailForm;
import io.micronaut.views.ViewsRenderer;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Singleton
public class EmailService {
    public static final String EMAIL_TEMPLATE_PATH = "templates/base-email/base-email.html";
    public static final String FORM_NAME = "template";
    private final ViewsRenderer<Map<String, EmailForm>, HttpRequest> viewsRenderer;
    EmailOperations emailOperations;

    public EmailService(EmailOperations emailOperations, ViewsRenderer<Map<String, EmailForm>, HttpRequest> viewsRenderer) {
        this.emailOperations = emailOperations;
        this.viewsRenderer = viewsRenderer;
    }

    public void sendTemplatedEmail(EmailForm emailForm) {
        var body = viewsRenderer.render(
            EMAIL_TEMPLATE_PATH,
            Collections.singletonMap(FORM_NAME, emailForm),
            null);

        String out;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            body.writeTo(outputStream);
            out = outputStream.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        emailOperations.sendHtmlBasedEmail(emailForm.recipient().email(), emailForm.email().subject(), out);
    }
}
