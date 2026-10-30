package com.posadskiy.user.web.handler;

import com.posadskiy.user.core.exception.AuthException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;

@Produces
@Singleton
public class AuthExceptionHandler implements ExceptionHandler<AuthException, HttpResponse<AuthExceptionHandler.Problem>> {

    @Override
    public HttpResponse<Problem> handle(HttpRequest request, AuthException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.status());
        return HttpResponse.status(status)
            .body(new Problem(status.getCode(), status.getReason(), exception.getMessage(), request.getPath()));
    }

    @Serdeable
    public record Problem(int status, String title, String detail, String instance) {
    }
}
