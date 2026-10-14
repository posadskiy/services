package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.core.config.AuthProperties;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/.well-known")
public class DiscoveryController {

    private final AuthProperties properties;

    public DiscoveryController(AuthProperties properties) {
        this.properties = properties;
    }

    @Get("/openid-configuration")
    public OpenIdConfiguration configuration() {
        String issuer = properties.jwt().issuer();
        return new OpenIdConfiguration(
                issuer,
                issuer + "/oauth2/authorize",
                issuer + "/oauth/token",
                issuer + "/keys",
                issuer + "/oauth/revoke",
                List.of("code"),
                List.of("authorization_code", "refresh_token", "client_credentials", "password"),
                List.of("openid", "email", "profile"),
                List.of("RS256", "ES256", "HS256"));
    }

    @Serdeable
    public record OpenIdConfiguration(
            String issuer,
            String authorization_endpoint,
            String token_endpoint,
            String jwks_uri,
            String revocation_endpoint,
            List<String> response_types_supported,
            List<String> grant_types_supported,
            List<String> scopes_supported,
            List<String> id_token_signing_alg_values_supported) {}
}
