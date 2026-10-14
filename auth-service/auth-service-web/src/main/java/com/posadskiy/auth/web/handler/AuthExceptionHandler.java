package com.posadskiy.auth.web.handler;

import com.posadskiy.auth.api.dto.ProblemDetail;
import com.posadskiy.auth.core.exception.AuthException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces
@Singleton
public class AuthExceptionHandler implements ExceptionHandler<AuthException, HttpResponse<ProblemDetail>> {

    private static final Logger LOG = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @Override
    public HttpResponse<ProblemDetail> handle(HttpRequest request, AuthException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.status());
        LOG.debug("auth error status={} path={}", exception.status(), request.getPath());
        ProblemDetail body = new ProblemDetail(
                "about:blank", status.getReason(), status.getCode(), exception.getMessage(), request.getPath());
        return HttpResponse.status(status).body(body);
    }
}
