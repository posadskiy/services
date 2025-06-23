package com.posadskiy.user.core.exception;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ErrorMessage(Boolean status, String message) {
}
