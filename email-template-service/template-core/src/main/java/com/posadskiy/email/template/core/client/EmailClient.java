package com.posadskiy.email.template.core.client;

import com.posadskiy.email.api.SendEmailForm;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("http://email-service.local")
public interface EmailClient extends EmailOperations {

    @Consumes(MediaType.APPLICATION_JSON)
    @Post("email/send/html")
    void sendHtmlBasedEmail(String authorization, SendEmailForm sendEmailForm);
}
