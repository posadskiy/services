package com.posadskiy.auth.core.utils;

import jakarta.inject.Singleton;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Singleton
public class PasswordMatcher {
    public static final int PASSWORD_STRENGTH = 16;
    public static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(PASSWORD_STRENGTH);

    public static boolean match(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
