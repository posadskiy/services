package com.posadskiy.auth.core.crypto;

import com.posadskiy.auth.core.config.AuthProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void hashesAndVerifies() {
        AuthProperties properties = new AuthProperties(null, new AuthProperties.Password(4), null, null, null);
        PasswordHasher hasher = new PasswordHasher(properties);
        hasher.warmup();
        String hash = hasher.hash("correcthorse");
        assertTrue(hasher.verify("correcthorse", hash));
        assertFalse(hasher.verify("wrong-password", hash));
        assertNotEquals("correcthorse", hash);
        hasher.dummyVerify("anything");
    }
}
