package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedEntity("users")
public class UserEntity {
    @Id
    @GeneratedValue
    @NonNull
    private Long id;

    @NonNull
    @NotBlank
    private String username;

    @NonNull
    @NotBlank
    private String email;

    @NonNull
    @NotNull
    private String passwordHash;

    @DateUpdated
    @NonNull
    @NotNull
    private LocalDateTime updatedAt;

    @DateCreated
    @NonNull
    @NotNull
    private LocalDateTime createdAt;

    public @NonNull Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public @NonNull @NotBlank String getUsername() {
        return username;
    }

    public void setUsername(@NonNull @NotBlank String username) {
        this.username = username;
    }

    public @NonNull @NotBlank String getEmail() {
        return email;
    }

    public void setEmail(@NonNull @NotBlank String email) {
        this.email = email;
    }

    public @NonNull @NotNull String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@NonNull @NotNull String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public @NonNull @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull @NotNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @NonNull @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull @NotNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
