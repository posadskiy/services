package com.posadskiy.email.web;

import com.posadskiy.email.resend.ResendProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;

/**
 * A production instance without these values would report healthy and then fail every send.
 * Refuse to start instead.
 */
@Context
@Requires(env = "prod")
public class ProdResendConfig {

    public ProdResendConfig(ResendProperties properties, @Value("${email.allowed-link-origins:}") String linkOrigins) {
        require(properties.apiKey(), "RESEND_API_KEY");
        require(properties.allowedFrom(), "RESEND_ALLOWED_FROM");
        require(linkOrigins, "EMAIL_ALLOWED_LINK_ORIGINS");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Production requires " + name);
        }
    }
}
