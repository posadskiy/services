package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.AuthException;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Singleton
public class CaptchaVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(CaptchaVerifier.class);

    private final String secret;
    private final HttpClient client;

    public CaptchaVerifier(AuthProperties properties) {
        String configured = properties.captcha().turnstileSecret();
        this.secret = configured == null ? "" : configured.trim();
        if (secret.isEmpty()) {
            LOG.warn("AUTH_TURNSTILE_SECRET is not set: sign-up and password-reset captcha is disabled");
        }
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public void verify(String token, String remoteIp) {
        if (secret.isEmpty()) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new AuthException("Captcha is required");
        }
        try {
            String form = "secret=" + encode(secret)
                + "&response=" + encode(token.trim())
                + (remoteIp == null || remoteIp.isBlank() ? "" : "&remoteip=" + encode(remoteIp));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(3))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || !response.body().contains("\"success\":true")) {
                throw new AuthException("Captcha was not accepted");
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("captcha verification failed", e);
            throw new AuthException("Captcha was not accepted");
        }
    }
}
