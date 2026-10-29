package com.posadskiy.email.resend;

public interface ResendHttp {

    int postEmails(String json, String idempotencyKey);
}
