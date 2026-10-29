package com.posadskiy.auth.core.mail;

import com.posadskiy.auth.core.exception.AuthException;
import io.micronaut.core.annotation.Nullable;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * HTTPS origins allowed in verification and password-reset links. Custom app schemes are rejected
 * because a mail client cannot open them.
 */
public final class EmailLinkOrigins {

    private EmailLinkOrigins() {
    }

    public static String resolve(@Nullable String requested, @Nullable String allowlist) {
        String candidate = blankToNull(requested);
        if (candidate == null) {
            throw new AuthException("Email link origin is required");
        }
        candidate = stripTrailingSlash(candidate);
        if (!isHttpsOrigin(candidate)) {
            throw new AuthException("Email link origin is not allowed");
        }
        Set<String> allowed = parse(allowlist);
        if (!allowed.contains(candidate)) {
            throw new AuthException("Email link origin is not allowed");
        }
        return candidate;
    }

    static Set<String> parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> allowed = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String value = stripTrailingSlash(part.trim());
            if (!value.isEmpty()) {
                allowed.add(value);
            }
        }
        return Set.copyOf(allowed);
    }

    private static boolean isHttpsOrigin(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (uri.getRawQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null) {
            return false;
        }
        String path = uri.getPath();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            return false;
        }
        if (uri.getHost() == null || uri.getScheme() == null) {
            return false;
        }
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }
        boolean local = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        return "http".equalsIgnoreCase(uri.getScheme()) && local;
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
