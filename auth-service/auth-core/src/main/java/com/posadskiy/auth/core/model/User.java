package com.posadskiy.auth.core.model;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record User(Long id, String username, String email, String password) {
}
