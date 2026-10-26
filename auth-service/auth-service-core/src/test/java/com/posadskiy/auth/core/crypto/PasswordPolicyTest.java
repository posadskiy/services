package com.posadskiy.auth.core.crypto;

import com.posadskiy.auth.core.exception.AuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void rejectsShortPasswords() {
        assertThrows(AuthException.class, () -> policy.validate("short", "a@b.c", "user"));
    }

    @Test
    void rejectsPasswordEqualToEmail() {
        assertThrows(AuthException.class, () -> policy.validate("person@x.com", "person@x.com", "user"));
    }

    @Test
    void acceptsLongEnoughPassword() {
        assertDoesNotThrow(() -> policy.validate("correcthorse", "a@b.c", "user"));
    }
}
