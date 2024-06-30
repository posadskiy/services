package com.posadskiy.email.controller.impl;

import com.posadskiy.email.controller.EmailController;
import com.posadskiy.email.dto.SendEmailForm;
import com.posadskiy.email.mapper.EmailMapper;
import com.posadskiy.email.service.EmailService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.tracing.annotation.ContinueSpan;

@Controller("email")
public class JavaMailEmailController implements EmailController {

    EmailService emailService;
    EmailMapper emailMapper;

    public JavaMailEmailController(EmailService emailService, EmailMapper emailMapper) {
        this.emailService = emailService;
        this.emailMapper = emailMapper;
    }

    @Post("send/text")
    @ContinueSpan
    public void sendText(@Body SendEmailForm form) {
        emailService.sendText(
            emailMapper.toModel(form)
        );
    }

    @Post("send/html")
    @ContinueSpan
    public void sendHtml(@Body SendEmailForm form) {
        emailService.sendHtml(
            emailMapper.toModel(form)
        );
    }
}
