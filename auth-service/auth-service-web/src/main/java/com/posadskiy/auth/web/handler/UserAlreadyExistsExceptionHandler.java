package com.posadskiy.auth.web.handler;

import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.ErrorMessage;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Produces
@Singleton
@Requires(classes = {AuthException.class, ExceptionHandler.class})
public class UserAlreadyExistsExceptionHandler
        implements ExceptionHandler<AuthException, HttpResponse<ErrorMessage>> {

    @SuppressWarnings("rawtypes")
    @Override
    public HttpResponse<ErrorMessage> handle(HttpRequest request, AuthException exception) {
        var message = new ErrorMessage(false, exception.getMessage());

        return HttpResponse.serverError(message).status(HttpStatus.BAD_REQUEST);
    }
}
