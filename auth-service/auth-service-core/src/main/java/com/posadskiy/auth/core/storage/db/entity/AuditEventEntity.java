package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;

import java.time.Instant;

@MappedEntity("audit_event")
public record AuditEventEntity(
    @Id @GeneratedValue @Nullable Long id,
    @Nullable @MappedProperty("user_id") Long userId,
    @MappedProperty("event_type") String eventType,
    @Nullable String detail,
    @DateCreated @Nullable Instant createdAt) {
}
