package com.posadskiy.auth.core.jwt;

import io.micronaut.security.token.claims.JtiGenerator;
import jakarta.inject.Singleton;
import java.util.UUID;

@Singleton
public class UuidJtiGenerator implements JtiGenerator {

    @Override
    public String generateJtiClaim() {
        return UUID.randomUUID().toString();
    }
}
