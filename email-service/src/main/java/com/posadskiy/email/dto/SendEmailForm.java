package com.posadskiy.email.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public record SendEmailForm(String to, String subject, String body) {
}
