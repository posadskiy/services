package com.posadskiy.auth.web;

import com.posadskiy.auth.core.config.AuthProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

@Context
@Singleton
@Requires(env = "prod")
public class ProdJwtKeys {

    public ProdJwtKeys(AuthProperties properties) {
        AuthProperties.Jwt jwt = properties.jwt();
        if (jwt.privateKeyPem() == null
            || jwt.privateKeyPem().isBlank()
            || jwt.publicKeyPem() == null
            || jwt.publicKeyPem().isBlank()) {
            throw new IllegalStateException(
                "Production must sign access tokens with AUTH_JWT_PRIVATE_KEY_PEM and AUTH_JWT_PUBLIC_KEY_PEM");
        }
    }
}
