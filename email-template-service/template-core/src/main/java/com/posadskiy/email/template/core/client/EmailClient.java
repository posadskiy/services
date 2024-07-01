package com.posadskiy.email.template.core.client;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Client("http://localhost:8090")
public interface EmailClient extends EmailOperations {

    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/email/send/html")
    void sendHtmlBasedEmail(String to, String subject, String body);
}
