package com.posadskiy.user.core.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.inject.Singleton;

@Singleton
public class PasswordEncoder {
    public static final int PASSWORD_STRENGTH = 10;

    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword");
        }
        return BCrypt.withDefaults().hashToString(PASSWORD_STRENGTH, rawPassword.toCharArray());
    }
}
