package com.posadskiy.email.controller;

import com.posadskiy.email.dto.SendEmailForm;

public interface EmailController {
    void sendText(SendEmailForm dto);
    void sendHtml(SendEmailForm dto);
}
