package com.posadskiy.auth.core.oauth;

import java.net.URI;

/**
 * Path and Secure flag for the OAuth browser-binding cookie.
 * The public base may include a prefix that Traefik strips, such as {@code /auth}.
 */
public final class OAuthBrowserCookie {

    private OAuthBrowserCookie() {}

    public static String path(String redirectBaseUrl) {
        String prefix = pathPrefix(redirectBaseUrl);
        return prefix.isEmpty() ? "/oauth2" : prefix + "/oauth2";
    }

    public static boolean secure(String redirectBaseUrl) {
        return redirectBaseUrl != null && redirectBaseUrl.regionMatches(true, 0, "https://", 0, "https://".length());
    }

    private static String pathPrefix(String redirectBaseUrl) {
        if (redirectBaseUrl == null || redirectBaseUrl.isBlank()) {
            return "";
        }
        try {
            String path = URI.create(redirectBaseUrl.trim()).getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return "";
            }
            return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
