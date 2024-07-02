package com.posadskiy.auth.core.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record SendEmail(String to, String subject, String body) {
}
