package com.posadskiy.email.web.controller;

import com.posadskiy.email.api.SendEmailForm;
import io.micronaut.http.HttpResponse;

public interface EmailController {
    HttpResponse<Void> sendText(String authorization, SendEmailForm dto);

    HttpResponse<Void> sendHtml(String authorization, SendEmailForm dto);
}
