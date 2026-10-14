package com.posadskiy.auth.core.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;

@Singleton
public class AuthMetrics {

    private final MeterRegistry registry;

    public AuthMetrics(@Nullable MeterRegistry registry) {
        this.registry = registry != null ? registry : new SimpleMeterRegistry();
    }

    public void loginSuccess(String method) {
        counter("auth.login.success", method);
    }

    public void loginFailure(String method) {
        counter("auth.login.failure", method);
    }

    public void refresh(String outcome) {
        registry.counter("auth.token.refresh", "outcome", outcome).increment();
    }

    public void oauth(String provider, String outcome) {
        registry.counter("auth.oauth", "provider", provider, "outcome", outcome).increment();
    }

    private void counter(String name, String method) {
        Counter.builder(name).tag("method", method).register(registry).increment();
    }
}
