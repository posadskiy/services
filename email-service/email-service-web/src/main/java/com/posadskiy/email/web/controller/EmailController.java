package com.posadskiy.email.web.controller;

import com.posadskiy.email.api.SendEmailForm;
import io.micronaut.http.HttpResponse;

public interface EmailController {
    HttpResponse<Void> sendText(SendEmailForm dto);

    HttpResponse<Void> sendHtml(SendEmailForm dto);
}
