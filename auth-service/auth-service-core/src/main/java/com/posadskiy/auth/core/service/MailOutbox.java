package com.posadskiy.auth.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.mail.TransactionalMailer;
import com.posadskiy.auth.core.storage.db.MailOutboxRepository;
import com.posadskiy.auth.core.storage.db.entity.MailOutboxEntity;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Durable hand-off to email-service. A message is stored first, then sent right away; if email-service
 * or Resend is unavailable the row stays and {@link #drain()} retries it with backoff. The idempotency key
 * is stable across retries, so a message that was accepted but not acknowledged is not delivered twice.
 */
@Singleton
public class MailOutbox {

    public static final int MAX_ATTEMPTS = 8;
    public static final Duration MAX_AGE = Duration.ofHours(24);

    private static final Logger LOG = LoggerFactory.getLogger(MailOutbox.class);
    private static final TypeReference<Map<String, String>> VARIABLES = new TypeReference<>() {};

    private final MailOutboxRepository repository;
    private final TransactionalMailer mailer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MailOutbox(MailOutboxRepository repository, TransactionalMailer mailer) {
        this.repository = repository;
        this.mailer = mailer;
    }

    static Duration backoff(int attempts) {
        long seconds = 30L << Math.min(Math.max(attempts, 0), 7);
        return Duration.ofSeconds(Math.min(seconds, 3600));
    }

    private static String truncate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    /**
     * Stores the message and tries once. Never throws: the caller's request must not fail because mail is late.
     */
    public void submit(TransactionalMail mail) {
        MailOutboxEntity row;
        try {
            row = repository.save(new MailOutboxEntity(
                    null,
                    mail.from(),
                    mail.to(),
                    mail.template(),
                    objectMapper.writeValueAsString(mail.variables()),
                    mail.idempotencyKey(),
                    mail.category(),
                    0,
                    Instant.now(),
                    null,
                    null));
        } catch (Exception e) {
            LOG.warn("mail could not be queued template={}", mail.template(), e);
            return;
        }
        attempt(row);
    }

    public void drain() {
        Instant now = Instant.now();
        for (MailOutboxEntity row :
                repository.findTop20ByAttemptsLessThanAndNextAttemptAtLessThanEqualOrderByNextAttemptAt(
                        MAX_ATTEMPTS, now)) {
            if (row.createdAt() != null && row.createdAt().isBefore(now.minus(MAX_AGE))) {
                continue;
            }
            attempt(row);
        }
    }

    private void attempt(MailOutboxEntity row) {
        try {
            Map<String, String> variables = objectMapper.readValue(row.variables(), VARIABLES);
            mailer.send(new TransactionalMail(
                    row.fromAddress(), row.toEmail(), row.template(), variables, row.idempotencyKey(), row.category()));
            repository.deleteById(row.id());
        } catch (Exception e) {
            MailOutboxEntity next = row.failed(Instant.now().plus(backoff(row.attempts())), truncate(e.getMessage()));
            repository.update(next);
            if (next.attempts() >= MAX_ATTEMPTS) {
                LOG.error(
                        "mail given up after {} attempts template={} category={}",
                        next.attempts(),
                        row.template(),
                        row.category());
            } else {
                LOG.warn("mail not accepted, will retry attempt={} template={}", next.attempts(), row.template());
            }
        }
    }
}
