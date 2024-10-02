package com.posadskiy.email.template.controller;

import com.posadskiy.email.template.api.EmailFormDto;
import com.posadskiy.email.template.core.service.EmailService;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.tracing.annotation.NewSpan;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("email/template")
public class EmailController {
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @Post("send/base")
    @NewSpan
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void sendBaseTemplatedEmail(@Header(AUTHORIZATION) String authorization, @Body EmailFormDto emailForm) {
        emailService.sendTemplatedEmail(authorization, emailForm);
    }
}
