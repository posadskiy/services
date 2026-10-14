package com.posadskiy.auth.core.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.posadskiy.auth.core.config.AuthProperties;
import io.micronaut.context.annotation.Factory;
import io.micronaut.security.token.claims.ClaimsAudienceProvider;
import io.micronaut.security.token.jwt.endpoints.JwkProvider;
import io.micronaut.security.token.jwt.signature.SignatureGeneratorConfiguration;
import io.micronaut.security.token.jwt.signature.ec.ECSignatureGenerator;
import io.micronaut.security.token.jwt.signature.ec.ECSignatureGeneratorConfiguration;
import io.micronaut.security.token.jwt.signature.secret.SecretSignature;
import io.micronaut.security.token.jwt.signature.secret.SecretSignatureConfiguration;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Factory
public class JwtCryptoFactory {

    @Singleton
    @Named("generator")
    SignatureGeneratorConfiguration signatureGenerator(AuthProperties properties) {
        AuthProperties.Jwt jwt = properties.getJwt();
        if (notBlank(jwt.getPrivateKeyPem()) && notBlank(jwt.getPublicKeyPem())) {
            return new ECSignatureGenerator(ecConfig(jwt));
        }
        String secret = jwt.getHmacSecret();
        if (!notBlank(secret)) {
            throw new IllegalStateException("Set AUTH_JWT_HMAC_SECRET (or JWT_GENERATOR_SIGNATURE_SECRET) "
                    + "or provide AUTH_JWT_PRIVATE_KEY_PEM and AUTH_JWT_PUBLIC_KEY_PEM");
        }
        SecretSignatureConfiguration configuration = new SecretSignatureConfiguration("generator");
        configuration.setSecret(secret);
        configuration.setJwsAlgorithm(JWSAlgorithm.HS256);
        return new SecretSignature(configuration);
    }

    @Singleton
    JwkProvider jwkProvider(AuthProperties properties, SignatureGeneratorConfiguration generator) {
        return () -> {
            if (generator instanceof ECSignatureGenerator) {
                return List.of(toJwk(properties, generator));
            }
            return List.of();
        };
    }

    @Singleton
    ClaimsAudienceProvider audienceProvider(AuthProperties properties) {
        return () -> List.of(properties.getJwt().getAudience());
    }

    private static JWK toJwk(AuthProperties properties, SignatureGeneratorConfiguration generator) {
        AuthProperties.Jwt jwt = properties.getJwt();
        if (generator instanceof ECSignatureGenerator) {
            ECKey.Builder builder = new ECKey.Builder(Curve.P_256, publicKey(jwt.getPublicKeyPem()))
                    .keyID("auth-es256")
                    .algorithm(JWSAlgorithm.ES256);
            return builder.build();
        }
        throw new IllegalStateException("HMAC secrets must not be published as JWKS");
    }

    private static ECSignatureGeneratorConfiguration ecConfig(AuthProperties.Jwt jwt) {
        ECPublicKey publicKey = publicKey(jwt.getPublicKeyPem());
        ECPrivateKey privateKey = privateKey(jwt.getPrivateKeyPem());
        return new ECSignatureGeneratorConfiguration() {
            @Override
            public ECPublicKey getPublicKey() {
                return publicKey;
            }

            @Override
            public ECPrivateKey getPrivateKey() {
                return privateKey;
            }

            @Override
            public JWSAlgorithm getJwsAlgorithm() {
                return JWSAlgorithm.ES256;
            }

            @Override
            public Optional<String> getKid() {
                return Optional.of("auth-es256");
            }
        };
    }

    public static ECPublicKey publicKey(String pem) {
        try {
            byte[] decoded = decodePem(pem, "PUBLIC KEY");
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid EC public key PEM", e);
        }
    }

    public static ECPrivateKey privateKey(String pem) {
        try {
            byte[] decoded = decodePem(pem, "PRIVATE KEY");
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid EC private key PEM", e);
        }
    }

    private static byte[] decodePem(String pem, String type) {
        String normalized = pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** Used by tests to mint a throwaway ES256 pair. */
    public static ECKey generateEs256ForTests() {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID("test").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
