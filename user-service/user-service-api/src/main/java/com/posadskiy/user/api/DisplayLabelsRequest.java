package com.posadskiy.user.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Batch lookup of member labels for internal services. Ids are decimal strings.
 */
@Serdeable
@Introspected
public record DisplayLabelsRequest(
    @NonNull @NotNull @Size(max = 100) List<@NotNull @Size(max = 32) String> ids) {
}
