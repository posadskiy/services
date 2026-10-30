package com.posadskiy.auth.core.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BreachedPasswordCheckerTest {

    @Test
    void findsAListedSuffix() {
        assertTrue(BreachedPasswordChecker.listed("ABCDEF:3\n", "ABCDEF"));
        assertFalse(BreachedPasswordChecker.listed("ABCDEF:0\n", "ABCDEF"));
        assertFalse(BreachedPasswordChecker.listed("OTHER:4\n", "ABCDEF"));
    }
}
