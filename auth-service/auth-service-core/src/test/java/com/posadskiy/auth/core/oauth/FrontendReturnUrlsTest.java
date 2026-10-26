package com.posadskiy.auth.core.oauth;

import com.posadskiy.auth.core.exception.OAuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrontendReturnUrlsTest {

    private static final String ALLOWED =
        "https://repeaty.posadskiy.com/oauth/callback,https://costy.posadskiy.com/oauth/callback";

    @Test
    void acceptsExactAllowlistedUrl() {
        assertEquals(
            "https://costy.posadskiy.com/oauth/callback",
            FrontendReturnUrls.resolve("https://costy.posadskiy.com/oauth/callback", null, ALLOWED));
    }

    @Test
    void rejectsUnknownUrl() {
        assertThrows(
            OAuthException.class,
            () -> FrontendReturnUrls.resolve("https://evil.example/oauth/callback", null, ALLOWED));
    }

    @Test
    void blankRequestUsesFallbackOnlyWhenAllowlistPermitsIt() {
        assertNull(FrontendReturnUrls.resolve("  ", "", ALLOWED));
        assertEquals(
            "https://repeaty.posadskiy.com/oauth/callback",
            FrontendReturnUrls.resolve(null, "https://repeaty.posadskiy.com/oauth/callback", ALLOWED));
    }
}
