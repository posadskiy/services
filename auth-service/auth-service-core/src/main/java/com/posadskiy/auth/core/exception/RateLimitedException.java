package com.posadskiy.auth.core.exception;

public class RateLimitedException extends AuthException {

    public RateLimitedException() {
        super("Too many requests", 429);
    }
}
