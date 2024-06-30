package com.posadskiy.email.web.controller;

import com.posadskiy.email.api.SendEmailForm;

public interface EmailController {
    void sendText(SendEmailForm dto);
    void sendHtml(SendEmailForm dto);
}
