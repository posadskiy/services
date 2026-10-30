package com.posadskiy.auth.core.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailNormalizerTest {

    @Test
    void lowercasesAndTrims() {
        assertEquals("user@example.com", EmailNormalizer.normalize("  User@Example.COM "));
        assertNull(EmailNormalizer.normalize(null));
    }
}
