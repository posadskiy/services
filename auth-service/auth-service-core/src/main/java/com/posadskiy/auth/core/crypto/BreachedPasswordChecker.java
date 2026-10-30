package com.posadskiy.auth.core.crypto;

import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.AuthException;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Singleton
public class BreachedPasswordChecker {

    private static final Logger LOG = LoggerFactory.getLogger(BreachedPasswordChecker.class);

    private final boolean enabled;
    private final HttpClient client;

    public BreachedPasswordChecker(AuthProperties properties) {
        this.enabled = properties.password().hibpEnabled();
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public void check(String password) {
        if (!enabled || password == null) {
            return;
        }
        try {
            String sha1 = sha1(password);
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                .header("Add-Padding", "true")
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.warn("breached-password check skipped status={}", response.statusCode());
                return;
            }
            if (listed(response.body(), suffix)) {
                throw new AuthException("This password has appeared in a data breach");
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("breached-password check skipped", e);
        }
    }

    static boolean listed(String body, String suffix) {
        if (body == null) {
            return false;
        }
        for (String line : body.split("\n")) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon < 0) {
                continue;
            }
            if (trimmed.substring(0, colon).equalsIgnoreCase(suffix)) {
                try {
                    return Integer.parseInt(trimmed.substring(colon + 1).trim()) > 0;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private static String sha1(String password) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(password.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest).toUpperCase();
    }
}
