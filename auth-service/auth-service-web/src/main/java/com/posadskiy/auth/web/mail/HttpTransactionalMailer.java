package com.posadskiy.auth.web.mail;

import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.mail.TransactionalMailer;
import io.micronaut.context.annotation.Requires;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;

@Singleton
@Requires(notEnv = "test")
public class HttpTransactionalMailer implements TransactionalMailer {

    private final EmailServiceClient client;
    private final TokenGenerator tokenGenerator;

    public HttpTransactionalMailer(EmailServiceClient client, TokenGenerator tokenGenerator) {
        this.client = client;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public void send(TransactionalMail mail) {
        String access = tokenGenerator
            .generateToken(
                Authentication.build("auth-service", List.of("ROLE_SERVICE"), Map.of("token_use", "service")),
                60)
            .orElseThrow(() -> new IllegalStateException("Unable to generate a service token"));
        client.send(
            "Bearer " + access,
            new EmailServiceClient.TransactionalEmailPayload(
                mail.from(),
                mail.to(),
                mail.template(),
                mail.variables(),
                mail.idempotencyKey(),
                mail.category()));
    }
}
