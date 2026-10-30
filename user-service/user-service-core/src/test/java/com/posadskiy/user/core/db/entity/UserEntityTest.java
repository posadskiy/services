package com.posadskiy.user.core.db.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void normalizesEmailWithoutChangingTheStoredAddress() {
        LocalDateTime now = LocalDateTime.now();
        UserEntity user = new UserEntity(
            123L,
            " Test@Example.com ",
            UserEntity.normalizedEmail(" Test@Example.com "),
            "hashedPassword",
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            true,
            now,
            now,
            null);

        assertEquals(123L, user.id());
        assertEquals(" Test@Example.com ", user.email());
        assertEquals("test@example.com", user.emailNormalized());
        assertEquals("hashedPassword", user.passwordHash());
        assertEquals(now, user.updatedAt());
        assertEquals(now, user.createdAt());
    }

    @Test
    void keepsNormalizedEmailEmptyWhenAddressIsMissing() {
        UserEntity user = UserEntity.account(null, null, null);

        assertNull(user.id());
        assertNull(user.email());
        assertNull(user.emailNormalized());
        assertNull(user.passwordHash());
    }

    @Test
    void equalityIncludesTheWholeRow() {
        UserEntity user1 = UserEntity.account(123L, "test@example.com", "hashedPassword");
        UserEntity user2 = UserEntity.account(123L, "test@example.com", "hashedPassword");
        UserEntity user3 = UserEntity.account(456L, "test@example.com", "hashedPassword");

        assertEquals(user1, user2);
        assertNotEquals(user1, user3);
        assertEquals(user1.hashCode(), user2.hashCode());
        assertNotEquals(user1.hashCode(), user3.hashCode());
    }
}
