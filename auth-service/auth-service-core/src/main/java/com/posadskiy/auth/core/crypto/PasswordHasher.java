package com.posadskiy.auth.core.crypto;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.posadskiy.auth.core.config.AuthProperties;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

/**
 * BCrypt via {@code at.favre.lib} (no Spring). Default cost 10 is ~50–150ms even on a 0.5 CPU
 * pod — well under a 1–2s login budget. Login taking ~10s was almost certainly event-loop
 * blocking and/or a cold database connection, not hashing cost.
 */
@Singleton
public class PasswordHasher {

    private final int cost;
    private volatile String dummyHash;

    public PasswordHasher(AuthProperties properties) {
        this.cost = properties.password().bcryptCost();
    }

    @PostConstruct
    void warmup() {
        this.dummyHash = hash(SecureTokens.randomUrl(16));
        verify("warmup-password-not-used", dummyHash);
    }

    public String hash(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("password required");
        }
        return BCrypt.withDefaults().hashToString(cost, rawPassword.toCharArray());
    }

    public boolean verify(String rawPassword, String encoded) {
        if (rawPassword == null || encoded == null || encoded.isBlank()) {
            verify("timing-pad", dummyHash());
            return false;
        }
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), encoded).verified;
    }

    /** Run a full bcrypt check even when the user does not exist, to equalise response times. */
    public void dummyVerify(String rawPassword) {
        verify(rawPassword == null ? "" : rawPassword, dummyHash());
    }

    private String dummyHash() {
        String hash = dummyHash;
        return hash != null ? hash : hash("dummy-password-placeholder");
    }
}
