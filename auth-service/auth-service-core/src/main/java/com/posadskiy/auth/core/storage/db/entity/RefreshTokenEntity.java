package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedEntity("refresh_token")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue
    @NonNull
    private Long id;

    @NonNull
    @NotBlank
    private String username;

    @NonNull
    @NotBlank
    private String refreshToken;

    @NonNull
    @NotNull
    private Boolean revoked;

    @DateCreated
    @NonNull
    @NotNull
    private LocalDateTime dateCreated;

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

    public @NonNull @NotBlank String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(@NonNull @NotBlank String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public @NonNull @NotNull Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(@NonNull @NotNull Boolean revoked) {
        this.revoked = revoked;
    }

    public @NonNull @NotNull LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(@NonNull @NotNull LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
