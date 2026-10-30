package com.posadskiy.auth.web;

import com.posadskiy.auth.core.service.MailOutbox;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries mail that email-service did not accept the first time.
 */
@Singleton
public class MailOutboxDrainer {

    private static final Logger LOG = LoggerFactory.getLogger(MailOutboxDrainer.class);

    private final MailOutbox outbox;

    public MailOutboxDrainer(MailOutbox outbox) {
        this.outbox = outbox;
    }

    @Scheduled(fixedDelay = "20s", initialDelay = "30s")
    public void drain() {
        try {
            outbox.drain();
        } catch (RuntimeException e) {
            LOG.warn("mail outbox drain failed", e);
        }
    }
}
