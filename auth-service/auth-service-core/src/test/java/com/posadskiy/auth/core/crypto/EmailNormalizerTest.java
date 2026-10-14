package com.posadskiy.auth.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void lowercasesAndTrims() {
        assertEquals("user@example.com", EmailNormalizer.normalize("  User@Example.COM "));
        assertNull(EmailNormalizer.normalize(null));
    }
}
