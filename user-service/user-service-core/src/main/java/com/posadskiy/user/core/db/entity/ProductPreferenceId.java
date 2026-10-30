package com.posadskiy.user.core.db.entity;

import io.micronaut.data.annotation.Embeddable;
import io.micronaut.data.annotation.MappedProperty;

@Embeddable
public record ProductPreferenceId(@MappedProperty("user_id") Long userId, String product) {
}
