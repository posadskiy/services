package com.posadskiy.auth.core.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.posadskiy.auth.core.exception.AuthException;
import org.junit.jupiter.api.Test;

class MailFromTest {

    @Test
    void grammarbarUsesItsOwnSender() {
        assertEquals(
                "Alpha <alpha@example.com>",
                MailFrom.resolve(
                        "https://grammarbar.com",
                        "Alpha <alpha@example.com>",
                        "Beta <beta@example.com>"));
    }

    @Test
    void otherOriginsUseThePosadskiySender() {
        assertEquals(
                "Beta <beta@example.com>",
                MailFrom.resolve(
                        "https://costy.posadskiy.com", "Alpha <alpha@example.com>", "Beta <beta@example.com>"));
        assertEquals(
                "Beta <beta@example.com>",
                MailFrom.resolve("http://localhost:3000", "Alpha <alpha@example.com>", "Beta <beta@example.com>"));
    }

    @Test
    void missingSenderIsRejected() {
        assertThrows(
                AuthException.class,
                () -> MailFrom.resolve("https://grammarbar.com", " ", "Beta <beta@example.com>"));
    }
}
