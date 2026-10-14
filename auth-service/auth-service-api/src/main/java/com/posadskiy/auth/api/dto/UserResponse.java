package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@Introspected
public record UserResponse(
        Long id,
        String username,
        String email,
        boolean emailVerified,
        @Nullable String pictureUrl,
        List<String> roles) {}
