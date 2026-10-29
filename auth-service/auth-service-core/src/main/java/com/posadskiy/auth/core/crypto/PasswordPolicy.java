package com.posadskiy.auth.core.crypto;

import com.posadskiy.auth.core.exception.AuthException;
import jakarta.inject.Singleton;

@Singleton
public class PasswordPolicy {

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
    }
}
