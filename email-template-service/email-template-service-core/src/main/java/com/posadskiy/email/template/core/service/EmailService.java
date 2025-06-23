package com.posadskiy.email.template.core.service;

import com.posadskiy.email.api.SendEmailForm;
import com.posadskiy.email.template.api.EmailFormDto;
import com.posadskiy.email.template.core.client.EmailOperations;
import com.posadskiy.email.template.core.client.UserClient;
import com.posadskiy.email.template.core.mapper.EmailFormMapper;
import io.micronaut.views.ViewsRenderer;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Singleton
public class EmailService {
    public static final String EMAIL_TEMPLATE_PATH = "templates/base-email/base-email.html";
    public static final String FORM_NAME = "template";

    private final UserClient userClient;
    private final EmailFormMapper emailFormMapper;
    private final ViewsRenderer viewsRenderer;
    private final EmailOperations emailOperations;

    public EmailService(UserClient userClient, EmailFormMapper emailFormMapper, EmailOperations emailOperations, ViewsRenderer viewsRenderer) {
        this.userClient = userClient;
        this.emailFormMapper = emailFormMapper;
        this.emailOperations = emailOperations;
        this.viewsRenderer = viewsRenderer;
    }

    public void sendTemplatedEmail(String authorization, EmailFormDto emailFormDto) {
        var user = userClient.getUserById(authorization, emailFormDto.recipient().id());

        var emailForm = emailFormMapper.toModel(emailFormDto, user);

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

        emailOperations.sendHtmlBasedEmail(authorization, new SendEmailForm(user.id(), emailForm.email().subject(), out));
    }
}
