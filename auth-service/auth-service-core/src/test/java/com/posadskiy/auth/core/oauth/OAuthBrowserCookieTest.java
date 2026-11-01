package com.posadskiy.auth.core.oauth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OAuthBrowserCookieTest {

    @Test
    void localBaseUsesOauthPath() {
        assertEquals("/oauth2", OAuthBrowserCookie.path("http://localhost:8100"));
        assertFalse(OAuthBrowserCookie.secure("http://localhost:8100"));
    }

    @Test
    void publicPrefixIsPartOfTheCookiePath() {
        assertEquals("/auth/oauth2", OAuthBrowserCookie.path("https://api.posadskiy.com/auth"));
        assertEquals("/auth", OAuthBrowserCookie.sessionPath("https://api.posadskiy.com/auth"));
        assertEquals("/", OAuthBrowserCookie.sessionPath("http://localhost:8100"));
        assertTrue(OAuthBrowserCookie.secure("https://api.posadskiy.com/auth"));
    }
}
