package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@Introspected
public record UserResponse(
        String id,
        String username,
        String email,
        boolean emailVerified,
        @Nullable String pictureUrl,
        List<String> roles) {

    /** Decimal form of the database id. This is the access token `sub`. */
    public static String idOf(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Account id is missing");
        }
        return Long.toString(id);
    }
}
