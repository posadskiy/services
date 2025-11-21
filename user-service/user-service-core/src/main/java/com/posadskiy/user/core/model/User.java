package com.posadskiy.user.core.model;

import io.micronaut.core.annotation.Introspected;
import java.util.List;

@Introspected
public record User(
        Long id,
        String username,
        String email,
        String password,
        Boolean emailVerified,
        String pictureUrl,
        String createdVia,
        List<String> authProviders) {

    public User(Long id, String username, String email, String password) {
        this(id, username, email, password, null, null, "local", List.of());
    }
}
