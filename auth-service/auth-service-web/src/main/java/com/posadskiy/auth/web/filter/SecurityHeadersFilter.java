package com.posadskiy.auth.web.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;

@ServerFilter("/**")
public class SecurityHeadersFilter implements Ordered {

    @ResponseFilter
    public void addHeaders(HttpRequest<?> request, MutableHttpResponse<?> response) {
        response.header("X-Content-Type-Options", "nosniff");
        response.header("Referrer-Policy", "no-referrer");
        response.header("X-Frame-Options", "DENY");
        response.header("Cache-Control", "no-store");
        if (request.isSecure()) {
            response.header("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
