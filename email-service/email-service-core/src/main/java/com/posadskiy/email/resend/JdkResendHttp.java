package com.posadskiy.email.resend;

import jakarta.inject.Singleton;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Singleton
public class JdkResendHttp implements ResendHttp {

    private final ResendProperties properties;
    private final HttpClient httpClient;

    public JdkResendHttp(ResendProperties properties) {
        this.properties = properties;
        this.httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    private static final Pattern ADDRESS = Pattern.compile("[^\\s<>\"',;]+@[^\\s<>\"',;]+");
    private static final long[] RETRY_DELAYS_MS = {300, 1200};

    static String excerpt(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = ADDRESS.matcher(body).replaceAll("<address>").replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }

    @Override
    public int postEmails(String json, String idempotencyKey) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new EmailDeliveryException("RESEND_API_KEY is not set");
        }
        String base = properties.apiUrl().endsWith("/")
            ? properties.apiUrl().substring(0, properties.apiUrl().length() - 1)
            : properties.apiUrl();
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/emails"))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + properties.apiKey())
            .header("Content-Type", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        for (int attempt = 0; ; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status / 100 == 2) {
                    return status;
                }
                if (!retryable(status) || attempt >= RETRY_DELAYS_MS.length) {
                    throw new EmailDeliveryException(
                        "Resend rejected the message: " + status + " " + excerpt(response.body()));
                }
            } catch (EmailDeliveryException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EmailDeliveryException("Resend request was interrupted", e);
            } catch (Exception e) {
                if (attempt >= RETRY_DELAYS_MS.length) {
                    throw new EmailDeliveryException("Resend request failed: " + e.getMessage(), e);
                }
            }
            pause(RETRY_DELAYS_MS[attempt]);
        }
    }

    /**
     * Resend honours the Idempotency-Key, so repeating a request that may have been accepted is safe.
     */
    private static boolean retryable(int status) {
        return status == 429 || status >= 500;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailDeliveryException("Resend request was interrupted", e);
        }
    }
}
