package com.posadskiy.auth.web;

import io.micronaut.http.HttpRequest;

public final class ClientAddresses {

    private ClientAddresses() {
    }

    /**
     * First address in {@code X-Forwarded-For}, which Traefik sets from the connecting client.
     */
    public static String key(HttpRequest<?> request) {
        String forwarded = request.getHeaders().get("X-Forwarded-For");
        if (forwarded != null) {
            for (String part : forwarded.split(",")) {
                String candidate = part.trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddress().getAddress().getHostAddress();
    }
}
