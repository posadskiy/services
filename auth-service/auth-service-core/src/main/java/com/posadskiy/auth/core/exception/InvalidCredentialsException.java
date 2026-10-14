package com.posadskiy.auth.core.exception;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super("Invalid credentials", 401);
    }
}
