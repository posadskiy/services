package com.posadskiy.auth.core.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.posadskiy.auth.core.SocialAuthConstants;
import com.posadskiy.auth.core.storage.db.entity.ExternalIdentityEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Singleton
public class JwtTokenService {

    private final JWSSigner signer;
    private final Duration accessTokenTtl;

    public JwtTokenService(
            @Value("${social.oauth.jwt.access-token-ttl:PT5M}") Duration accessTokenTtl,
            @Value("${JWT_GENERATOR_SIGNATURE_SECRET}") String secret)
            throws Exception {
        this.accessTokenTtl = accessTokenTtl;
        this.signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TokenDetails generate(UserEntity user, ExternalIdentityEntity identity) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plus(accessTokenTtl);

            JWTClaimsSet claims =
                    new JWTClaimsSet.Builder()
                            .subject(String.valueOf(user.getId()))
                            .issuer("auth-service")
                            .issueTime(Date.from(now))
                            .expirationTime(Date.from(expiresAt))
                            .claim("email", user.getEmail())
                            .claim(SocialAuthConstants.CLAIM_EMAIL_VERIFIED, user.getEmailVerified())
                            .claim(SocialAuthConstants.CLAIM_PICTURE, user.getPictureUrl())
                            .claim(SocialAuthConstants.ATTR_PROVIDER, identity.getProvider())
                            .claim(SocialAuthConstants.ATTR_EXTERNAL_SUBJECT, identity.getProviderUserId())
                            .build();

            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);

            return new TokenDetails(jwt.serialize(), Duration.between(now, expiresAt).toSeconds());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate JWT token", e);
        }
    }

    public record TokenDetails(String accessToken, long expiresInSeconds) {}
}

