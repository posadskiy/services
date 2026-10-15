package com.posadskiy.auth.core.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OAuthBrowserCookieTest {

    @Test
    void localBaseUsesOauthPath() {
        assertEquals("/oauth2", OAuthBrowserCookie.path("http://localhost:8100"));
        assertFalse(OAuthBrowserCookie.secure("http://localhost:8100"));
    }

    @Test
    void publicPrefixIsPartOfTheCookiePath() {
        assertEquals("/auth/oauth2", OAuthBrowserCookie.path("https://api.posadskiy.com/auth"));
        assertTrue(OAuthBrowserCookie.secure("https://api.posadskiy.com/auth"));
    }
}
