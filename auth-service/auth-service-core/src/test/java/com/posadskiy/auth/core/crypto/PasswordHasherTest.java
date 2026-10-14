package com.posadskiy.auth.core.crypto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.posadskiy.auth.core.config.AuthProperties;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void hashesAndVerifies() {
        AuthProperties properties = new AuthProperties();
        properties.getPassword().setBcryptCost(4);
        PasswordHasher hasher = new PasswordHasher(properties);
        hasher.warmup();
        String hash = hasher.hash("correcthorse");
        assertTrue(hasher.verify("correcthorse", hash));
        assertFalse(hasher.verify("wrong-password", hash));
        assertNotEquals("correcthorse", hash);
        hasher.dummyVerify("anything");
    }
}
