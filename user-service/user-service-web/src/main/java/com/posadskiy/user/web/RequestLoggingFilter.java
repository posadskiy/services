package com.posadskiy.user.web;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * One INFO line per API request, written inside the request's trace span, so the JSON log carries
 * traceId / spanId and a trace in Tempo can be matched with its log line in Loki.
 * Health checks and metric scrapes are skipped; the query string is not logged.
 */
@ServerFilter(ServerFilter.MATCH_ALL_PATTERN)
class RequestLoggingFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String STARTED_AT = "request.startedAtNanos";

    @RequestFilter
    void start(HttpRequest<?> request) {
        request.setAttribute(STARTED_AT, System.nanoTime());
    }

    @ResponseFilter
    void log(HttpRequest<?> request, HttpResponse<?> response) {
        String path = request.getPath();
        if (path.contains("/health") || path.endsWith("/prometheus")) {
            return;
        }
        long millis = request.getAttribute(STARTED_AT, Long.class)
            .map(start -> (System.nanoTime() - start) / 1_000_000)
            .orElse(-1L);
        SpanContext span = Span.current().getSpanContext();
        if (!span.isValid()) {
            LOG.info("{} {} -> {} in {} ms", request.getMethod(), path, response.code(), millis);
            return;
        }
        try (MDC.MDCCloseable traceId = MDC.putCloseable("traceId", span.getTraceId());
             MDC.MDCCloseable spanId = MDC.putCloseable("spanId", span.getSpanId())) {
            LOG.info("{} {} -> {} in {} ms", request.getMethod(), path, response.code(), millis);
        }
    }
}
