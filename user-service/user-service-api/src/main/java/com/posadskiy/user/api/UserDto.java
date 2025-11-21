package com.posadskiy.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Serdeable
@Introspected
public record UserDto(
        @Nullable Long id,
        @NonNull @NotBlank String username,
        @NonNull @NotNull String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) @Nullable String password,
        @Nullable Boolean emailVerified,
        @Nullable String pictureUrl,
        @Nullable String createdVia,
        @Nullable List<String> authProviders) {

    public static UserDto legacy(String id, String username, String email, String password) {
        return new UserDto(parseId(id), username, email, password, null, null, null, List.of());
    }

    private static Long parseId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
