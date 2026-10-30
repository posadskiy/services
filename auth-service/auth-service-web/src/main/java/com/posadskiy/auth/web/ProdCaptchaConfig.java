package com.posadskiy.auth.web;

import com.posadskiy.auth.core.config.AuthProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;

/**
 * Without a Turnstile secret the captcha check passes every request, so sign-up and password reset would send
 * mail to any address with no bot protection. Refuse to start in production instead of failing open.
 */
@Context
@Requires(env = "prod")
public class ProdCaptchaConfig {

    public ProdCaptchaConfig(AuthProperties properties) {
        String secret = properties.captcha().turnstileSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Production requires AUTH_TURNSTILE_SECRET");
        }
    }
}
