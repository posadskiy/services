package com.posadskiy.auth.core.mail;

public interface TransactionalMailer {

    void send(TransactionalMail mail);
}
