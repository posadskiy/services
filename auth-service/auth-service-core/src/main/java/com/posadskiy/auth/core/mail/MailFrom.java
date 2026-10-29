package com.posadskiy.auth.core.mail;

import com.posadskiy.auth.core.exception.AuthException;
import io.micronaut.core.annotation.Nullable;

/**
 * Grammarbar mail uses {@code AUTH_EMAIL_FROM_GRAMMARBAR}. Every other allowed origin, including
 * Costy, Repeaty, and localhost, uses {@code AUTH_EMAIL_FROM_POSADSKIY}.
 */
public final class MailFrom {

    public static final String GRAMMARBAR_ORIGIN = "https://grammarbar.com";

    private MailFrom() {}

    public static String resolve(String origin, @Nullable String grammarbarFrom, @Nullable String posadskiyFrom) {
        String from = GRAMMARBAR_ORIGIN.equals(origin) ? grammarbarFrom : posadskiyFrom;
        if (from == null || from.isBlank() || from.indexOf('\r') >= 0 || from.indexOf('\n') >= 0) {
            throw new AuthException("Email from-address is not configured");
        }
        return from.trim();
    }
}
