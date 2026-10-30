package com.posadskiy.auth.core.config;

import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * An enabled machine client with a blank secret would authenticate as itself.
 * Fail the application start instead of serving that client. In production the secret must also be long
 * enough that guessing it is not realistic.
 */
@Singleton
public class EnabledClientSecretCheck {

    static final int MIN_PRODUCTION_SECRET_LENGTH = 32;

    @Inject
    public EnabledClientSecretCheck(List<OAuthClientProperties> clients, Environment environment) {
        this(clients, environment.getActiveNames().contains("prod"));
    }

    public EnabledClientSecretCheck(List<OAuthClientProperties> clients) {
        this(clients, false);
    }

    EnabledClientSecretCheck(List<OAuthClientProperties> clients, boolean production) {
        for (OAuthClientProperties client : clients) {
            if (!client.enabled()) {
                continue;
            }
            String secret = client.clientSecret();
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                    "Enabled client '" + client.name() + "' requires a non-blank client secret");
            }
            if (production && secret.length() < MIN_PRODUCTION_SECRET_LENGTH) {
                throw new IllegalStateException("Enabled client '" + client.name() + "' requires a client secret of at least "
                    + MIN_PRODUCTION_SECRET_LENGTH + " characters in production");
            }
        }
    }
}
