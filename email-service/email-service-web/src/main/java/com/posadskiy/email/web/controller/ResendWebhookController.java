package com.posadskiy.email.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.email.resend.ResendProperties;
import com.svix.Webhook;
import com.svix.exceptions.EmptyWebhookSecretException;
import com.svix.exceptions.WebhookVerificationException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/email/webhooks")
@ExecuteOn(TaskExecutors.BLOCKING)
public class ResendWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(ResendWebhookController.class);

    private final ResendProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResendWebhookController(ResendProperties properties) {
        this.properties = properties;
    }

    @Post("/resend")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> receive(HttpRequest<?> request) {
        String payload = request.getBody(String.class).orElse("");
        try {
            verify(payload, request);
        } catch (WebhookVerificationException | EmptyWebhookSecretException | IllegalArgumentException e) {
            return HttpResponse.status(HttpStatus.BAD_REQUEST);
        }
        String event = eventType(payload);
        if ("email.bounced".equals(event) || "email.complained".equals(event)) {
            LOG.warn("resend event {}", event);
        } else {
            LOG.info("resend event {}", event);
        }
        return HttpResponse.noContent();
    }

    private void verify(String payload, HttpRequest<?> request)
            throws WebhookVerificationException, EmptyWebhookSecretException {
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw new WebhookVerificationException("Missing webhook signature");
        }
        Map<String, List<String>> headers = new HashMap<>();
        header(headers, "svix-id", request.getHeaders().get("svix-id"));
        header(headers, "svix-timestamp", request.getHeaders().get("svix-timestamp"));
        header(headers, "svix-signature", request.getHeaders().get("svix-signature"));
        new Webhook(secret).verify(payload, headers);
    }

    private static void header(Map<String, List<String>> headers, String name, String value) {
        if (value != null) {
            headers.put(name, List.of(value));
        }
    }

    private String eventType(String payload) {
        try {
            JsonNode type = objectMapper.readTree(payload).get("type");
            if (type != null && type.isTextual() && !type.asText().isBlank()) {
                return type.asText();
            }
        } catch (JsonProcessingException e) {
            LOG.warn("resend webhook body was not json");
        }
        return "unknown";
    }
}
