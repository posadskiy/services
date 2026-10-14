package com.posadskiy.auth.core.oauth;

import com.posadskiy.auth.core.crypto.SecureTokens;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class Pkce {

    private Pkce() {}

    public static String verifier() {
        return SecureTokens.randomUrl(32);
    }

    public static String challengeS256(String verifier) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create PKCE challenge", e);
        }
    }

    public static boolean matches(String verifier, String challenge) {
        if (verifier == null || challenge == null) {
            return verifier == null && challenge == null;
        }
        return SecureTokens.equalsConstantTime(challengeS256(verifier), challenge);
    }
}
