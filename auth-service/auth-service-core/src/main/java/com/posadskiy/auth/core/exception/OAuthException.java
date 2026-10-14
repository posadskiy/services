package com.posadskiy.auth.core.exception;

public class OAuthException extends AuthException {

    public OAuthException(String message) {
        super(message, 400);
    }

    public OAuthException(String message, int status) {
        super(message, status);
    }
}
