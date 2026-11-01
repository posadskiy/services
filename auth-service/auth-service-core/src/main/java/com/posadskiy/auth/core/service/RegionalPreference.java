package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.exception.AuthException;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

/** BCP 47 language tag and IANA time zone. Invalid input is rejected, never stored. */
public final class RegionalPreference {
    private static final Pattern LOCALE = Pattern.compile("^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8}){0,3}$");

    private RegionalPreference() {
    }

    public static String locale(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim().replace('_', '-');
        if (trimmed.length() > 35 || !LOCALE.matcher(trimmed).matches()) {
            throw new AuthException("Locale is invalid");
        }
        Locale parsed = Locale.forLanguageTag(trimmed);
        if (parsed.getLanguage().isBlank() || "und".equals(parsed.getLanguage())) {
            throw new AuthException("Locale is invalid");
        }
        return parsed.toLanguageTag();
    }

    public static String timeZone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 64) {
            throw new AuthException("Time zone is invalid");
        }
        try {
            String id = ZoneId.of(trimmed).getId();
            if (!id.equals("UTC") && !id.contains("/")) {
                throw new AuthException("Time zone is invalid");
            }
            return id;
        } catch (DateTimeException exception) {
            throw new AuthException("Time zone is invalid");
        }
    }

    /** Sign-up keeps going when a browser sends a value this runtime does not accept. */
    public static String localeOrNull(String value) {
        try {
            return locale(value);
        } catch (AuthException exception) {
            return null;
        }
    }

    public static String timeZoneOrNull(String value) {
        try {
            return timeZone(value);
        } catch (AuthException exception) {
            return null;
        }
    }
}
