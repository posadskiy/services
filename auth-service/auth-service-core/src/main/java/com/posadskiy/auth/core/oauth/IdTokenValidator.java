package com.posadskiy.auth.core.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.posadskiy.auth.core.config.OAuthProviderProperties;
import com.posadskiy.auth.core.exception.OAuthException;
import jakarta.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class IdTokenValidator {

    private final Map<String, ConfigurableJWTProcessor<SecurityContext>> processors = new ConcurrentHashMap<>();

    public JWTClaimsSet validate(OAuthProviderProperties provider, String idToken, String nonce) {
        if (idToken == null || idToken.isBlank()) {
            throw new OAuthException("Provider did not return an ID token");
        }
        if (provider.jwksUri() == null || provider.jwksUri().isBlank()) {
            throw new OAuthException("Provider JWKS URI is not configured");
        }
        try {
            JWTClaimsSet claims = processor(provider).process(idToken, null);
            if (provider.issuer() != null && !provider.issuer().equals(claims.getIssuer())) {
                throw new OAuthException("ID token issuer mismatch");
            }
            List<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(provider.clientId())) {
                throw new OAuthException("ID token audience mismatch");
            }
            String tokenNonce = claims.getStringClaim("nonce");
            if (nonce != null && !nonce.equals(tokenNonce)) {
                throw new OAuthException("ID token nonce mismatch");
            }
            return claims;
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException("Invalid ID token");
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> processor(OAuthProviderProperties provider) {
        return processors.computeIfAbsent(provider.name(), name -> {
            try {
                ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
                var source = JWKSourceBuilder.create(
                                URI.create(provider.jwksUri()).toURL())
                        .retrying(true)
                        .build();
                processor.setJWSKeySelector(
                        new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.RS256, JWSAlgorithm.ES256), source));
                return processor;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to initialise JWKS client for " + name, e);
            }
        });
    }
}
