package com.posadskiy.email.controller;

import com.posadskiy.email.dto.SendEmailForm;

public interface EmailController {
    void send(SendEmailForm dto);
}
