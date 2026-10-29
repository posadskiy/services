package com.posadskiy.auth.web.mail;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import java.util.Map;

@Client(id = "email-service")
@Requires(notEnv = "test")
public interface EmailServiceClient {

    @Post("/email/transactional")
    void send(@Header(HttpHeaders.AUTHORIZATION) String authorization, @Body TransactionalEmailPayload body);

    @io.micronaut.serde.annotation.Serdeable
    record TransactionalEmailPayload(
            String from,
            String to,
            String template,
            Map<String, String> variables,
            String idempotencyKey,
            String category) {}
}
