package com.posadskiy.user.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class UserServiceLabelTest {

    @Test
    void prefersDisplayName() {
        assertEquals("Ada", UserService.labelFrom(" Ada ", "ada@example.com"));
    }

    @Test
    void fallsBackToEmailLocalPart() {
        assertEquals("ada", UserService.labelFrom("  ", "ada@example.com"));
        assertEquals("ada", UserService.labelFrom(null, "ada@example.com"));
    }

    @Test
    void returnsNullWhenNothingUsable() {
        assertNull(UserService.labelFrom(null, null));
        assertNull(UserService.labelFrom(" ", " "));
    }
}
