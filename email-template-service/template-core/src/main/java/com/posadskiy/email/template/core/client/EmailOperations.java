package com.posadskiy.email.template.core.client;

import com.posadskiy.email.api.SendEmailForm;

public interface EmailOperations {
    void sendHtmlBasedEmail(String authorization, SendEmailForm sendEmailForm);
}
