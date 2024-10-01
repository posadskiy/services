package com.posadskiy.email.web.controller.impl;

import com.posadskiy.email.api.SendEmailForm;
import com.posadskiy.email.service.EmailService;
import com.posadskiy.email.web.controller.EmailController;
import com.posadskiy.email.web.mapper.EmailMapper;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.tracing.annotation.ContinueSpan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static io.micronaut.http.HttpHeaders.AUTHORIZATION;

@Controller("email")
@ExecuteOn(TaskExecutors.BLOCKING)
public class JavaMailEmailController implements EmailController {

    EmailService emailService;
    EmailMapper emailMapper;

    public JavaMailEmailController(EmailService emailService, EmailMapper emailMapper) {
        this.emailService = emailService;
        this.emailMapper = emailMapper;
    }

    @Post("send/text")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    @ContinueSpan
    @Operation(
        summary = "Send text-based email",
        tags = "email",
        description = "Sending text-based email's body with subject to provided email address",
        responses = {
            @ApiResponse(responseCode = "204", description = "Email has been sent successfully")
        }
    )
    public HttpResponse<Void> sendText(@Header(AUTHORIZATION) String authorization,
        @RequestBody(description = "Email-related details", required = true)
        @Body
        @Valid
        SendEmailForm form) {
        emailService.sendText(authorization, 
            emailMapper.toModel(form)
        );

        return HttpResponse.noContent();
    }

    @Post("send/html")
    @Consumes(MediaType.APPLICATION_JSON)
    @Status(HttpStatus.NO_CONTENT)
    @ContinueSpan
    @Operation(
        summary = "Send HTML-based email",
        tags = "email",
        description = "Sending HTML-based email's body with subject to provided email address",
        responses = {
            @ApiResponse(responseCode = "204", description = "Email has been sent successfully")
        }
    )
    public HttpResponse<Void> sendHtml(@Header(AUTHORIZATION) String authorization,
        @RequestBody(description = "Email-related details", required = true)
        @Body
        @NotNull
        @Valid SendEmailForm form) {
        emailService.sendHtml(authorization,
            emailMapper.toModel(form)
        );

        return HttpResponse.noContent();
    }
}
