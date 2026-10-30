package com.posadskiy.user.core.model;

import io.micronaut.core.annotation.Introspected;

import java.time.LocalDateTime;
import java.util.List;

@Introspected
public record User(
    Long id,
    String email,
    String password,
    Boolean emailVerified,
    String pictureUrl,
    String createdVia,
    List<String> authProviders,
    Boolean active,
    LocalDateTime deactivatedAt,
    String displayName,
    String locale,
    String timeZone,
    String avatarUrl) {

    public User(Long id, String email, String password) {
        this(id, email, password, null, null, "local", List.of(), true, null, null, null, null, null);
    }

    public User(
        Long id,
        String email,
        String password,
        Boolean emailVerified,
        String pictureUrl,
        String createdVia,
        List<String> authProviders,
        Boolean active,
        LocalDateTime deactivatedAt) {
        this(
            id,
            email,
            password,
            emailVerified,
            pictureUrl,
            createdVia,
            authProviders,
            active,
            deactivatedAt,
            null,
            null,
            null,
            null);
    }
}
