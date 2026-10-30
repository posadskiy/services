package com.posadskiy.email.resend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.email.api.TransactionalEmailRequest;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ResendTransactionalSender {

    private final ResendProperties properties;
    private final ResendHttp http;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<String> allowedLinkOrigins;

    @Inject
    public ResendTransactionalSender(
        ResendProperties properties, ResendHttp http, @Value("${email.allowed-link-origins:}") String allowedLinkOrigins) {
        this.properties = properties;
        this.http = http;
        this.allowedLinkOrigins = origins(allowedLinkOrigins);
    }

    public ResendTransactionalSender(ResendProperties properties, ResendHttp http) {
        this(properties, http, "");
    }

    private static Set<String> origins(String raw) {
        LinkedHashSet<String> origins = new LinkedHashSet<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String value = part.trim().toLowerCase(Locale.ROOT);
                if (!value.isEmpty()) {
                    origins.add(value.endsWith("/") ? value.substring(0, value.length() - 1) : value);
                }
            }
        }
        return origins;
    }

    /**
     * A caller holding a service token must not be able to put an arbitrary link in a message sent from our domain.
     * With no origins configured (local development) the check is off; production refuses to start without them.
     */
    private void checkLinks(TransactionalEmailRequest request) {
        if (allowedLinkOrigins.isEmpty()) {
            return;
        }
        for (String key : LINK_KEYS) {
            String url = request.variables().get(key);
            if (url != null && !allowedLinkOrigins.contains(origin(url))) {
                throw new LinkNotAllowedException("link origin is not allowed");
            }
        }
    }

    private static String origin(String url) {
        try {
            URI uri = new URI(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            String origin = uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT);
            return uri.getPort() < 0 ? origin : origin + ":" + uri.getPort();
        } catch (java.net.URISyntaxException e) {
            return "";
        }
    }

    private static final List<String> LINK_KEYS = List.of("VERIFY_URL", "RESET_URL");

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
            case "change-email" -> "Confirm your new email";
            case "new-login" -> "New sign-in to your account";
            default -> template;
        };
    }

    private static String text(TransactionalEmailRequest request) {
        String url = request.variables().getOrDefault(urlKey(request.template()), "");
        return switch (request.template()) {
            case "password-reset" -> "We received a request to reset your password.\n\n" + url
                + "\n\nThis link expires in 1 hour. If you did not ask for a reset, you can ignore this message.\n";
            case "verify-email", "change-email" -> "Confirm your email address.\n\n" + url
                + "\n\nThis link expires in 24 hours. If you did not ask for this, you can ignore this message.\n";
            case "new-login" ->
                "A new sign-in used " + request.variables().getOrDefault("DEVICE", "a new device") + ".\n";
            default -> url;
        };
    }

    private static String urlKey(String template) {
        return "password-reset".equals(template) ? "RESET_URL" : "VERIFY_URL";
    }

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\{([A-Za-z0-9_]+)}}}");

    /**
     * Single pass, so a value that itself looks like a placeholder is never substituted again.
     */
    static String render(String html, Map<String, String> variables) {
        return PLACEHOLDER.matcher(html).replaceAll(match -> {
            String key = match.group(1);
            return Matcher.quoteReplacement(variables.containsKey(key) ? escape(variables.get(key)) : match.group());
        });
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

    public void send(TransactionalEmailRequest request) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new EmailDeliveryException("RESEND_API_KEY is not set");
        }
        if (!allowed(properties.allowedFrom()).contains(mailbox(request.from()))) {
            throw new EmailDeliveryException("from address is not allowed");
        }
        checkLinks(request);
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
        html = render(html, request.variables());
        return new HtmlMail(
            request.from().trim(),
            List.of(request.to()),
            subject(request.template()),
            html,
            text(request),
            List.of(new Tag("category", request.category())));
    }

    private record HtmlMail(String from, List<String> to, String subject, String html, String text, List<Tag> tags) {
    }

    private record Payload(String from, List<String> to, Template template, List<Tag> tags) {
    }

    private record Template(String id, Map<String, String> variables) {
    }

    private record Tag(String name, String value) {
    }
}
