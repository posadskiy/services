package com.posadskiy.email.template.core.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record Button(String text, String link) {
}
