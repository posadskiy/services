package com.posadskiy.email.template.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record EmailFormDto(Email email, Recipient recipient, Content content) {
}

