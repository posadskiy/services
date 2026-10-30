package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

import java.time.Instant;

@MappedEntity("passkey_credential")
public record PasskeyCredentialEntity(
    @Id @GeneratedValue @Nullable Long id,
    @MappedProperty("user_id") Long userId,
    @MappedProperty("credential_id") String credentialId,
    @MappedProperty("public_key_cose") String publicKeyCose,
    @MappedProperty("signature_count") long signatureCount,
    @MappedProperty("user_handle") String userHandle,
    @Nullable String label,
    @DateCreated @Nullable Instant createdAt,
    @Nullable @MappedProperty("last_used_at") Instant lastUsedAt) {

    public PasskeyCredentialEntity used(long signatureCount, Instant at) {
        return new PasskeyCredentialEntity(
            id, userId, credentialId, publicKeyCose, signatureCount, userHandle, label, createdAt, at);
    }
}
