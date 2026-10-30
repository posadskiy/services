package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

@MappedEntity("email_suppression")
public record EmailSuppressionEntity(
    @Id @MappedProperty("email_normalized") String emailNormalized,
    @DateCreated @Nullable Instant createdAt) {
}
