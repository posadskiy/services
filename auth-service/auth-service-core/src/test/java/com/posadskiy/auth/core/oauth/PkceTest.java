package com.posadskiy.auth.core.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PkceTest {

    @Test
    void challengeMatchesVerifier() {
        String verifier = Pkce.verifier();
        String challenge = Pkce.challengeS256(verifier);
        assertTrue(Pkce.matches(verifier, challenge));
        assertFalse(Pkce.matches("other", challenge));
    }
}
