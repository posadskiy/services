package com.posadskiy.user.web;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.security.authentication.Authentication;

public final class AccountAccess {

    private AccountAccess() {
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
