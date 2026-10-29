package com.posadskiy.email.resend;

import jakarta.inject.Singleton;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Singleton
public class JdkResendHttp implements ResendHttp {

    private final ResendProperties properties;
    private final HttpClient httpClient;

    public JdkResendHttp(ResendProperties properties) {
        this.properties = properties;
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
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
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status / 100 != 2) {
                throw new EmailDeliveryException(
                        "Resend rejected the message: " + status + " " + excerpt(response.body()));
            }
            return status;
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailDeliveryException("Resend request was interrupted", e);
        } catch (Exception e) {
            throw new EmailDeliveryException("Resend request failed: " + e.getMessage(), e);
        }
    }

    static String excerpt(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }
}
