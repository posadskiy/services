package com.posadskiy.auth.core.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.posadskiy.auth.core.exception.AuthException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void rejectsShortPasswords() {
        assertThrows(AuthException.class, () -> policy.validate("short", "a@b.c"));
    }

    @Test
    void rejectsPasswordEqualToEmail() {
        assertThrows(AuthException.class, () -> policy.validate("person@x.com", "person@x.com"));
    }

    @Test
    void acceptsLongEnoughPassword() {
        assertDoesNotThrow(() -> policy.validate("correcthorse", "a@b.c"));
    }
}
