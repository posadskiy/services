package com.posadskiy.user.web;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.security.authentication.Authentication;

public final class AccountAccess {

    private AccountAccess() {
    }

    /**
     * Operations that change identity or erase an account are owned by auth-service. Users reach them there.
     */
    public static void serviceOnly(Authentication authentication) {
        if (!authentication.getRoles().contains("ROLE_SERVICE")) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "This operation is restricted to internal services");
        }
    }

    /** Batch display-labels. Issued to the Costy profile machine client only. */
    public static void labelsReader(Authentication authentication) {
        if (!authentication.getRoles().contains("ROLE_USER_LABELS")) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "This operation is restricted to label readers");
        }
    }

    public static void selfOrService(Authentication authentication, String userId) {
        if (authentication.getRoles().contains("ROLE_SERVICE")) {
            return;
        }
        if (!authentication.getName().equals(userId)) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "You can only access your own account");
        }
    }
}
