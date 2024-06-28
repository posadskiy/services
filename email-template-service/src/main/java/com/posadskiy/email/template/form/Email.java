package com.posadskiy.email.template.form;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record Email(String subject) {
}
