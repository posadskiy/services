package com.posadskiy.auth.core.exception;

public class AuthException extends RuntimeException {

    private final int status;

    public AuthException(String message) {
        this(message, 400);
    }

    public AuthException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
