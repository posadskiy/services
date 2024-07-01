package com.posadskiy.email.api;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Serdeable
@Introspected
public record SendEmailForm(
    @NotBlank
    @Schema(title = "Email's recipient", example = "support@posadskiy.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = "^.*@.*$")
    String to,

    @NotBlank
    @Schema(title = "Email's subject", example = "Email from swagger-ui", requiredMode = Schema.RequiredMode.REQUIRED)
    String subject,

    @NotBlank
    @Schema(title = "Email's body", example = "Test email from swagger-ui", requiredMode = Schema.RequiredMode.REQUIRED)
    String body
) {
}
