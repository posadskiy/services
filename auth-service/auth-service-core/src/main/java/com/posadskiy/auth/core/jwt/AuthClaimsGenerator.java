package com.posadskiy.auth.core.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import com.posadskiy.auth.core.config.AuthProperties;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.claims.ClaimsAudienceProvider;
import io.micronaut.security.token.claims.JtiGenerator;
import io.micronaut.security.token.config.TokenConfiguration;
import io.micronaut.security.token.jwt.generator.claims.JWTClaimsSetGenerator;
import jakarta.inject.Singleton;

@Singleton
@Replaces(JWTClaimsSetGenerator.class)
public class AuthClaimsGenerator extends JWTClaimsSetGenerator {

    private final AuthProperties properties;

    public AuthClaimsGenerator(
            TokenConfiguration tokenConfiguration,
            @Nullable JtiGenerator jwtIdGenerator,
            @Nullable ClaimsAudienceProvider claimsAudienceProvider,
            ApplicationConfiguration applicationConfiguration,
            AuthProperties properties) {
        super(tokenConfiguration, jwtIdGenerator, claimsAudienceProvider, applicationConfiguration);
        this.properties = properties;
    }

    @Override
    protected void populateIss(JWTClaimsSet.Builder builder) {
        builder.issuer(properties.jwt().issuer());
    }

    @Override
    protected void populateWithAuthentication(JWTClaimsSet.Builder builder, Authentication authentication) {
        super.populateWithAuthentication(builder, authentication);
        builder.claim("token_use", "access");
    }
}
