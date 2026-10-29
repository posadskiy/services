package com.posadskiy.email.web.controller;

import com.posadskiy.email.api.TransactionalEmailRequest;
import com.posadskiy.email.resend.EmailDeliveryException;
import com.posadskiy.email.resend.ResendTransactionalSender;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/email")
@ExecuteOn(TaskExecutors.BLOCKING)
public class TransactionalEmailController {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionalEmailController.class);

    private final ResendTransactionalSender sender;

    public TransactionalEmailController(ResendTransactionalSender sender) {
        this.sender = sender;
    }

    @Post("/transactional")
    @Secured("ROLE_SERVICE")
    public HttpResponse<Void> send(@Body @Valid TransactionalEmailRequest request) {
        try {
            sender.send(request);
        } catch (EmailDeliveryException e) {
            LOG.warn("transactional email rejected template={} reason={}", request.template(), e.getMessage());
            return HttpResponse.status(HttpStatus.BAD_GATEWAY);
        }
        return HttpResponse.noContent();
    }
}
