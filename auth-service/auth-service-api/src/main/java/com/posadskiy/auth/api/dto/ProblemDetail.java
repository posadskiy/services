package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        @Nullable String instance) {}
