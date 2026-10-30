package com.posadskiy.user.core.db.entity;

import io.micronaut.data.annotation.EmbeddedId;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

@MappedEntity("product_preference")
public record ProductPreferenceEntity(
    @EmbeddedId ProductPreferenceId id,
    @MappedProperty("marketing_email") boolean marketingEmail,
    String settings) {
}
