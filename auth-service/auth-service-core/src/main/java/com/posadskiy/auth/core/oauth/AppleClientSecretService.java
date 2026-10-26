package com.posadskiy.auth.core.oauth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.posadskiy.auth.core.config.OAuthProviderProperties;
import com.posadskiy.auth.core.jwt.JwtCryptoFactory;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Date;

@Singleton
public class AppleClientSecretService {

    public String clientSecret(OAuthProviderProperties provider) {
        if (!"apple".equalsIgnoreCase(provider.name())) {
            return provider.clientSecret();
        }
        if (provider.applePrivateKeyPem() == null || provider.appleTeamId() == null || provider.appleKeyId() == null) {
            return provider.clientSecret();
        }
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(provider.appleTeamId())
                .subject(provider.clientId())
                .audience("https://appleid.apple.com")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(86400 * 180L)))
                .build();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(provider.appleKeyId())
                .type(JOSEObjectType.JWT)
                .build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(JwtCryptoFactory.privateKey(provider.applePrivateKeyPem())));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to mint Apple client secret", e);
        }
    }
}
