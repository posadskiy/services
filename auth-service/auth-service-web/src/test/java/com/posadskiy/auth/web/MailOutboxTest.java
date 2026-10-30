package com.posadskiy.auth.web;

import static org.junit.jupiter.api.Assertions.*;

import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.service.ClientContext;
import com.posadskiy.auth.core.service.MailOutbox;
import com.posadskiy.auth.core.service.TokenService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.MailOutboxRepository;
import com.posadskiy.auth.core.storage.db.entity.MailOutboxEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest(transactional = false)
class MailOutboxTest extends PostgresMicronautTest {

    private static final String ORIGIN = "https://grammarbar.com";

    @Inject
    MailOutbox outbox;

    @Inject
    MailOutboxRepository repository;

    @Inject
    CapturingTransactionalMailer mailer;

    @Inject
    UserAccountService userAccountService;

    @Inject
    TokenService tokenService;

    @BeforeEach
    void reset() {
        mailer.clear();
        repository.deleteAll();
    }

    private static TransactionalMail mail(String key) {
        return new TransactionalMail(
                "Alpha <alpha@example.com>",
                "person@example.com",
                "verify-email",
                Map.of("VERIFY_URL", "https://grammarbar.com/verify-email?token=abc"),
                key,
                "verify_email");
    }

    private void makeDue() {
        for (MailOutboxEntity row : repository.findAll()) {
            repository.update(new MailOutboxEntity(
                    row.id(),
                    row.fromAddress(),
                    row.toEmail(),
                    row.template(),
                    row.variables(),
                    row.idempotencyKey(),
                    row.category(),
                    row.attempts(),
                    Instant.now().minusSeconds(1),
                    row.lastError(),
                    row.createdAt()));
        }
    }

    @Test
    void acceptedMailLeavesNothingBehind() {
        outbox.submit(mail("outbox-ok"));

        assertEquals(1, mailer.sent().size());
        assertEquals(0, repository.count());
    }

    @Test
    void mailThatEmailServiceRejectsIsRetriedWithTheSameKey() {
        mailer.failNext(1);
        outbox.submit(mail("outbox-retry"));

        assertTrue(mailer.sent().isEmpty());
        List<MailOutboxEntity> queued = repository.findAll();
        assertEquals(1, queued.size());
        assertEquals(1, queued.getFirst().attempts());
        assertTrue(queued.getFirst().nextAttemptAt().isAfter(Instant.now()));

        outbox.drain();
        assertTrue(mailer.sent().isEmpty(), "not due yet");

        makeDue();
        outbox.drain();

        assertEquals(1, mailer.sent().size());
        assertEquals("outbox-retry", mailer.sent().getFirst().idempotencyKey());
        assertEquals(0, repository.count());
    }

    @Test
    void mailIsGivenUpAfterTheLastAttempt() {
        mailer.failNext(100);
        outbox.submit(mail("outbox-dead"));
        for (int i = 1; i < MailOutbox.MAX_ATTEMPTS; i++) {
            makeDue();
            outbox.drain();
        }
        assertEquals(MailOutbox.MAX_ATTEMPTS, repository.findAll().getFirst().attempts());

        makeDue();
        mailer.failNext(0);
        outbox.drain();
        assertTrue(mailer.sent().isEmpty());
    }

    @Test
    void signInAnnouncesOnlyADeviceTheAccountHasNotUsedBefore() {
        UserEntity user = userAccountService.register("device@example.com", "correcthorse-device");
        mailer.clear();

        tokenService.issueForUser(user, "password", null, new ClientContext("10.0.0.1", "Firefox", ORIGIN));
        assertTrue(mailer.sent().isEmpty(), "first sign-in is not announced");

        tokenService.issueForUser(user, "password", null, new ClientContext("10.0.0.1", "Firefox", ORIGIN));
        assertTrue(mailer.sent().isEmpty(), "same user agent");

        tokenService.issueForUser(user, "password", null, new ClientContext("10.0.0.2", null, ORIGIN));
        assertTrue(mailer.sent().isEmpty(), "unknown user agent cannot be told apart");

        tokenService.issueForUser(user, "password", null, new ClientContext("10.0.0.3", "Safari", ORIGIN));
        assertEquals(1, mailer.sent().size());
        assertEquals("new-login", mailer.sent().getFirst().template());
        assertEquals("Safari", mailer.sent().getFirst().variables().get("DEVICE"));
    }
}
