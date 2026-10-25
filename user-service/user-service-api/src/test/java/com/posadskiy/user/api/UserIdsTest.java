package com.posadskiy.user.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UserIdsTest {

    @Test
    void roundTripsADatabaseIdAsADecimalString() {
        assertEquals("42", UserIds.of(42L));
        assertEquals(42L, UserIds.parse("42"));
    }

    @Test
    void rejectsNonCanonicalIds() {
        assertThrows(IllegalArgumentException.class, () -> UserIds.parse("01"));
        assertThrows(IllegalArgumentException.class, () -> UserIds.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> UserIds.parse(null));
    }
}
