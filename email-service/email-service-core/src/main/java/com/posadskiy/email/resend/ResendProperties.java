package com.posadskiy.email.resend;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;

@ConfigurationProperties("resend")
public record ResendProperties(
        @Nullable String apiKey, @Nullable String allowedFrom, String apiUrl, @Nullable String webhookSecret) {}
