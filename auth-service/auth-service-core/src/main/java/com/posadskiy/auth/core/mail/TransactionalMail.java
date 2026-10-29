package com.posadskiy.auth.core.mail;

import java.util.Map;

public record TransactionalMail(
        String from,
        String to,
        String template,
        Map<String, String> variables,
        String idempotencyKey,
        String category) {}
