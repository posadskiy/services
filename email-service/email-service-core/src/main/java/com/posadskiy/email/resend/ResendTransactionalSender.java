package com.posadskiy.email.resend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.email.api.TransactionalEmailRequest;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Singleton
public class ResendTransactionalSender {

    private final ResendProperties properties;
    private final ResendHttp http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResendTransactionalSender(ResendProperties properties, ResendHttp http) {
        this.properties = properties;
        this.http = http;
    }

    public void send(TransactionalEmailRequest request) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new EmailDeliveryException("RESEND_API_KEY is not set");
        }
        if (!allowed(properties.allowedFrom()).contains(mailbox(request.from()))) {
            throw new EmailDeliveryException("from address is not allowed");
        }
        String json = body(request);
        int status;
        try {
            status = http.postEmails(json, request.idempotencyKey());
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new EmailDeliveryException("Resend request failed: " + e, e);
        }
        if (status / 100 != 2) {
            throw new EmailDeliveryException("Resend rejected the message: " + status);
        }
    }

    private String body(TransactionalEmailRequest request) {
        Object payload = rendered(request);
        if (payload == null) {
            payload = new Payload(
                    request.from().trim(),
                    List.of(request.to()),
                    new Template(request.template(), request.variables()),
                    List.of(new Tag("category", request.category())));
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new EmailDeliveryException("Resend request could not be encoded", e);
        }
    }

    private HtmlMail rendered(TransactionalEmailRequest request) {
        String html = template(request.template());
        if (html == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : request.variables().entrySet()) {
            html = html.replace("{{{" + entry.getKey() + "}}}", escape(entry.getValue()));
        }
        return new HtmlMail(
                request.from().trim(),
                List.of(request.to()),
                subject(request.template()),
                html,
                text(request),
                List.of(new Tag("category", request.category())));
    }

    private static String template(String name) {
        if (name == null || !name.matches("[a-z0-9-]+")) {
            return null;
        }
        String path = "/templates/" + name + ".html";
        try (InputStream in = ResendTransactionalSender.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new EmailDeliveryException("Email template could not be read", e);
        }
    }

    private static String subject(String template) {
        return switch (template) {
            case "password-reset" -> "Reset your password";
            case "verify-email" -> "Confirm your email";
            default -> template;
        };
    }

    private static String text(TransactionalEmailRequest request) {
        String url = request.variables().getOrDefault(urlKey(request.template()), "");
        return switch (request.template()) {
            case "password-reset" ->
                    "We received a request to reset your password.\n\n" + url
                            + "\n\nThis link expires in 1 hour. If you did not ask for a reset, you can ignore this message.\n";
            case "verify-email" ->
                    "Confirm your email address to finish creating your account.\n\n" + url
                            + "\n\nThis link expires in 24 hours. If you did not create an account, you can ignore this message.\n";
            default -> url;
        };
    }

    private static String urlKey(String template) {
        return "password-reset".equals(template) ? "RESET_URL" : "VERIFY_URL";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static Set<String> allowed(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new EmailDeliveryException("RESEND_ALLOWED_FROM is not set");
        }
        LinkedHashSet<String> addresses = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String value = part.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                addresses.add(value);
            }
        }
        if (addresses.isEmpty()) {
            throw new EmailDeliveryException("RESEND_ALLOWED_FROM is not set");
        }
        return addresses;
    }

    private static String mailbox(String from) {
        if (from == null || from.isBlank() || from.indexOf('\r') >= 0 || from.indexOf('\n') >= 0) {
            throw new EmailDeliveryException("from address is not allowed");
        }
        int open = from.lastIndexOf('<');
        int close = from.lastIndexOf('>');
        String address = open >= 0 && close > open ? from.substring(open + 1, close) : from;
        address = address.trim().toLowerCase(Locale.ROOT);
        if (address.isEmpty() || address.indexOf('@') < 0 || address.indexOf(' ') >= 0) {
            throw new EmailDeliveryException("from address is not allowed");
        }
        return address;
    }

    private record HtmlMail(
            String from, List<String> to, String subject, String html, String text, List<Tag> tags) {}

    private record Payload(String from, List<String> to, Template template, List<Tag> tags) {}

    private record Template(String id, Map<String, String> variables) {}

    private record Tag(String name, String value) {}
}
