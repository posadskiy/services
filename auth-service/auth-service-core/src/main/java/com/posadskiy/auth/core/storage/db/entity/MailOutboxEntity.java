package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;
import java.time.Instant;

@MappedEntity("mail_outbox")
public record MailOutboxEntity(
        @Id @GeneratedValue @Nullable Long id,
        @MappedProperty("from_address") String fromAddress,
        @MappedProperty("to_email") String toEmail,
        String template,
        String variables,
        @MappedProperty("idempotency_key") String idempotencyKey,
        String category,
        int attempts,
        @MappedProperty("next_attempt_at") Instant nextAttemptAt,
        @Nullable @MappedProperty("last_error") String lastError,
        @DateCreated @Nullable Instant createdAt) {

    public MailOutboxEntity failed(Instant next, String error) {
        return new MailOutboxEntity(
                id,
                fromAddress,
                toEmail,
                template,
                variables,
                idempotencyKey,
                category,
                attempts + 1,
                next,
                error,
                createdAt);
    }
}
