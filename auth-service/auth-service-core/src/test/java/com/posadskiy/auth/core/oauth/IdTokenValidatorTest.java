package com.posadskiy.auth.core.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.posadskiy.auth.core.config.OAuthProviderProperties;
import com.posadskiy.auth.core.exception.OAuthException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdTokenValidatorTest {

    private RSAKey rsaKey;
    private HttpServer server;
    private OAuthProviderProperties provider;
    private IdTokenValidator validator;

    @BeforeEach
    void startJwks() throws Exception {
        rsaKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        String jwks = new JWKSet(rsaKey.toPublicJWK()).toString();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = jwks.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        provider = new OAuthProviderProperties(
            "google",
            true,
            "test-google-client",
            null,
            null,
            null,
            "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks",
            "https://accounts.google.com",
            null,
            null,
            null,
            null,
            null);
        validator = new IdTokenValidator();
    }

    @AfterEach
    void stopJwks() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void acceptsValidRs256IdToken() throws Exception {
        JWTClaimsSet claims = validator.validate(provider, signedToken("abc"), "abc");
        assertEquals("google-user-1", claims.getSubject());
    }

    @Test
    void rejectsNonceMismatch() throws Exception {
        String token = signedToken("expected-nonce");
        assertThrows(OAuthException.class, () -> validator.validate(provider, token, "other-nonce"));
    }

    @Test
    void rejectsAudienceMismatch() throws Exception {
        String token = signedToken("abc", "someone-else", provider.issuer());
        assertThrows(OAuthException.class, () -> validator.validate(provider, token, "abc"));
    }

    @Test
    void rejectsIssuerMismatch() throws Exception {
        String token = signedToken("abc", provider.clientId(), "https://evil.example");
        assertThrows(OAuthException.class, () -> validator.validate(provider, token, "abc"));
    }

    private String signedToken(String nonce) throws Exception {
        return signedToken(nonce, provider.clientId(), provider.issuer());
    }

    private String signedToken(String nonce, String audience, String issuer) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .subject("google-user-1")
            .claim("nonce", nonce)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(120)))
            .build();
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(rsaKey.getKeyID())
                .build(),
            claims);
        jwt.sign(new RSASSASigner(rsaKey.toPrivateKey()));
        return jwt.serialize();
    }
}
