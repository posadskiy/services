package com.posadskiy.auth.web;

import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.mail.TransactionalMailer;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
@Requires(env = "test")
public class CapturingTransactionalMailer implements TransactionalMailer {

    private final List<TransactionalMail> sent = new CopyOnWriteArrayList<>();
    private volatile int failures;

    @Override
    public void send(TransactionalMail mail) {
        if (failures > 0) {
            failures--;
            throw new IllegalStateException("email-service is down");
        }
        sent.add(mail);
    }

    /** The next {@code count} calls to {@link #send} throw, as if email-service were unreachable. */
    public void failNext(int count) {
        failures = count;
    }

    public List<TransactionalMail> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        failures = 0;
        sent.clear();
    }
}
