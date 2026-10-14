package com.posadskiy.auth.core.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Nullable;
import java.util.List;

@EachProperty("auth.clients")
public record OAuthClientProperties(
        @Parameter String name,
        boolean enabled,
        @Nullable String clientId,
        @Nullable String clientSecret,
        @Nullable List<String> roles) {

    public OAuthClientProperties {
        if (clientId == null || clientId.isBlank()) {
            clientId = name;
        }
        roles = roles == null || roles.isEmpty() ? List.of("ROLE_SERVICE") : List.copyOf(roles);
    }
}
