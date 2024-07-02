package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.SendEmailForm;
import io.micronaut.http.HttpResponse;

public interface EmailController {
    HttpResponse<Void> sendText(SendEmailForm dto);

    HttpResponse<Void> sendHtml(SendEmailForm dto);
}
