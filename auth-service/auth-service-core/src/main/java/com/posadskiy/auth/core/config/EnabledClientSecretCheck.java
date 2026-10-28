package com.posadskiy.auth.core.config;

import jakarta.inject.Singleton;
import java.util.List;

/**
 * An enabled machine client with a blank secret would authenticate as itself.
 * Fail the application start instead of serving that client.
 */
@Singleton
public class EnabledClientSecretCheck {

    public EnabledClientSecretCheck(List<OAuthClientProperties> clients) {
        for (OAuthClientProperties client : clients) {
            if (client.enabled() && (client.clientSecret() == null || client.clientSecret().isBlank())) {
                throw new IllegalStateException(
                        "Enabled client '" + client.name() + "' requires a non-blank client secret");
            }
        }
    }
}
