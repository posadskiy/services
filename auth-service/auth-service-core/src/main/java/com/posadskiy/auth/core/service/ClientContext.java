package com.posadskiy.auth.core.service;

import io.micronaut.core.annotation.Nullable;

public record ClientContext(@Nullable String ip, @Nullable String userAgent, @Nullable String linkOrigin) {

    public ClientContext {
        if (ip != null && ip.length() > 64) {
            ip = ip.substring(0, 64);
        }
        if (userAgent != null && userAgent.length() > 512) {
            userAgent = userAgent.substring(0, 512);
        }
    }
}
