package com.posadskiy.auth.core.oauth;

import com.posadskiy.auth.core.exception.OAuthException;
import io.micronaut.core.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

/** Exact-match allowlist for the browser URL auth-service redirects to after a provider callback. */
public final class FrontendReturnUrls {

    private FrontendReturnUrls() {}

    public static String resolve(@Nullable String requested, @Nullable String fallback, @Nullable String allowlist) {
        Set<String> allowed = parse(allowlist);
        if (requested == null || requested.isBlank()) {
            if (fallback == null || fallback.isBlank()) {
                return null;
            }
            if (!allowed.isEmpty() && !allowed.contains(fallback)) {
                throw new OAuthException("Return URL is not allowed");
            }
            return fallback;
        }
        String candidate = requested.trim();
        if (!allowed.contains(candidate)) {
            throw new OAuthException("Return URL is not allowed");
        }
        return candidate;
    }

    static Set<String> parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> allowed = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                allowed.add(value);
            }
        }
        return Set.copyOf(allowed);
    }
}
