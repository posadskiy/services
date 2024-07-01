package com.posadskiy.email.template.controller;

import com.posadskiy.email.template.api.EmailForm;
import com.posadskiy.email.template.core.service.EmailService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.tracing.annotation.NewSpan;

@Controller("email/template")
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @Post("send/base")
    @NewSpan
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void sendBaseTemplatedEmail(@Body EmailForm emailForm) {
        emailService.sendTemplatedEmail(emailForm);
    }
}
