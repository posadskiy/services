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

    @Override
    public void send(TransactionalMail mail) {
        sent.add(mail);
    }

    public List<TransactionalMail> sent() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
