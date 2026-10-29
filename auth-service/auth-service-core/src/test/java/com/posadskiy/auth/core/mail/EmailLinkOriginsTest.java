package com.posadskiy.auth.core.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.posadskiy.auth.core.exception.AuthException;
import org.junit.jupiter.api.Test;

class EmailLinkOriginsTest {

    private static final String ALLOWED = "https://grammarbar.com,https://costy.posadskiy.com,http://localhost:3000";

    @Test
    void usesTheRequestedOriginWhenItIsAllowlisted() {
        assertEquals("https://costy.posadskiy.com", EmailLinkOrigins.resolve("https://costy.posadskiy.com/", ALLOWED));
    }

    @Test
    void rejectsAMissingOrigin() {
        assertThrows(AuthException.class, () -> EmailLinkOrigins.resolve(null, ALLOWED));
        assertThrows(AuthException.class, () -> EmailLinkOrigins.resolve("  ", ALLOWED));
    }

    @Test
    void rejectsOriginsOutsideTheAllowlistAndNonHttpsSchemes() {
        assertThrows(AuthException.class, () -> EmailLinkOrigins.resolve("https://evil.example", ALLOWED));
        assertThrows(AuthException.class, () -> EmailLinkOrigins.resolve("costy://oauth", ALLOWED));
        assertThrows(
                AuthException.class, () -> EmailLinkOrigins.resolve("https://grammarbar.com/verify-email", ALLOWED));
    }
}
