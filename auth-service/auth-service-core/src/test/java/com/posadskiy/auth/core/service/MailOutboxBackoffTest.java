package com.posadskiy.auth.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MailOutboxBackoffTest {

    @Test
    void backoffDoublesAndIsCapped() {
        assertEquals(Duration.ofSeconds(30), MailOutbox.backoff(0));
        assertEquals(Duration.ofSeconds(60), MailOutbox.backoff(1));
        assertEquals(Duration.ofSeconds(240), MailOutbox.backoff(3));
        assertEquals(Duration.ofSeconds(3600), MailOutbox.backoff(7));
        assertEquals(Duration.ofSeconds(3600), MailOutbox.backoff(50));
    }
}
