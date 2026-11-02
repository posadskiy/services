package com.posadskiy.auth.api.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/** One-time code for {@code POST /oauth2/finalize}; only the holder of the matching verifier can redeem it. */
@Serdeable
@Introspected
public record AppCodeResponse(String code) {
}
