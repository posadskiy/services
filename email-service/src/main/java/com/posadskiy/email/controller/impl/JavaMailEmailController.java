package com.posadskiy.email.controller.impl;

import com.posadskiy.email.controller.EmailController;
import com.posadskiy.email.dto.SendEmailForm;
import com.posadskiy.email.mapper.EmailMapper;
import com.posadskiy.email.service.EmailService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.tracing.annotation.NewSpan;

@Controller("email")
public class JavaMailEmailController implements EmailController {

    EmailService emailService;
    EmailMapper emailMapper;

    public JavaMailEmailController(EmailService emailService, EmailMapper emailMapper) {
        this.emailService = emailService;
        this.emailMapper = emailMapper;
    }

    @Post("send")
    @NewSpan
    public void send(@Body SendEmailForm form) {
        emailService.send(
            emailMapper.toModel(form)
        );
    }
}
