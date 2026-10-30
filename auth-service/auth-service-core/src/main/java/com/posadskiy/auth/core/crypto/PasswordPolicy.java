package com.posadskiy.auth.core.crypto;

import com.posadskiy.auth.core.exception.AuthException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PasswordPolicy {

    private final BreachedPasswordChecker breached;

    public PasswordPolicy() {
        this(null);
    }

    @Inject
    public PasswordPolicy(BreachedPasswordChecker breached) {
        this.breached = breached;
    }

    public void validate(String password, String email) {
        if (password == null || password.length() < 10) {
            throw new AuthException("Password must be at least 10 characters");
        }
        if (password.length() > 72) {
            throw new AuthException("Password must be at most 72 characters");
        }
        if (email != null && password.equalsIgnoreCase(email)) {
            throw new AuthException("Password must not match the email address");
        }
        if (breached != null) {
            breached.check(password);
        }
    }
}
