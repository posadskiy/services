package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;
import jakarta.validation.constraints.NotBlank;
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

    @Nullable
    private String passwordHash;

    @MappedProperty("email_verified")
    private Boolean emailVerified = Boolean.FALSE;

    @MappedProperty("picture_url")
    private String pictureUrl;

    @MappedProperty("last_login_at")
    private LocalDateTime lastLoginAt;

    @DateUpdated
    private LocalDateTime updatedAt;

    @DateCreated
    private LocalDateTime createdAt;

    @Version
    private Long version;

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

    public @Nullable String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@Nullable String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
