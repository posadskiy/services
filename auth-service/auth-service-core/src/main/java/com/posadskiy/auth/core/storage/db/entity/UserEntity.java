package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@MappedEntity("users")
public class UserEntity {

    @Id
    @GeneratedValue
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
    private LocalDateTime updatedAt;

    @DateCreated
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
