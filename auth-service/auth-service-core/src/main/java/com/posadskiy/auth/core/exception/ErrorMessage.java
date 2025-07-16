package com.posadskiy.auth.core.exception;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ErrorMessage(Boolean status, String message) {}
