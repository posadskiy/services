package com.posadskiy.email.template.core.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record Recipient(String name, String email) {
}
