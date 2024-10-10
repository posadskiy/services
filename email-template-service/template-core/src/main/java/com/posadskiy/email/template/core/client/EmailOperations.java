package com.posadskiy.email.template.core.client;

import com.posadskiy.email.api.SendEmailForm;
import io.micronaut.http.HttpResponse;

public interface EmailOperations {
    HttpResponse<Void> sendHtmlBasedEmail(String authorization, SendEmailForm sendEmailForm);
}
