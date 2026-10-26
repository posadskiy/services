package com.posadskiy.user.api;

/**
 * Decimal string form of a user id. The database column stays {@code BIGINT}.
 */
public final class UserIds {

    private UserIds() {
    }

    public static String of(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User id is missing");
        }
        return Long.toString(id);
    }

    public static Long parse(String id) {
        if (id == null || id.isEmpty() || !id.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("Invalid user id");
        }
        try {
            Long value = Long.valueOf(id);
            if (!id.equals(Long.toString(value))) {
                throw new IllegalArgumentException("Invalid user id");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user id", e);
        }
    }
}
