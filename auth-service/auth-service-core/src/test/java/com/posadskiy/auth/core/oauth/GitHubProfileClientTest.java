package com.posadskiy.auth.core.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.auth.core.exception.OAuthException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubProfileClientTest {

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> userAgent = new AtomicReference<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/user", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] body =
                "{\"id\":42,\"login\":\"octocat\",\"name\":null,\"email\":null,\"avatar_url\":\"https://avatars.githubusercontent.com/u/42\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/emails", exchange -> {
            byte[] body =
                "[{\"email\":\"other@example.com\",\"primary\":false,\"verified\":true},{\"email\":\"primary@example.com\",\"primary\":true,\"verified\":true},{\"email\":\"hidden@example.com\",\"primary\":false,\"verified\":false}]"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void usesThePrimaryVerifiedEmail() {
        int port = server.getAddress().getPort();
        GitHubProfileClient client = new GitHubProfileClient(
            new OAuthOutboundClient(),
            new ObjectMapper(),
            "http://127.0.0.1:" + port + "/user",
            "http://127.0.0.1:" + port + "/emails");

        ExternalProfile profile = client.load("gho_token");

        assertEquals("github", profile.provider());
        assertEquals("42", profile.providerUserId());
        assertEquals("primary@example.com", profile.email());
        assertTrue(profile.emailVerified());
        assertEquals("octocat", profile.displayName());
        assertEquals("https://avatars.githubusercontent.com/u/42", profile.pictureUrl());
        assertEquals("Bearer gho_token", authorization.get());
        assertEquals("auth-service", userAgent.get());
    }

    @Test
    void fallsBackToThePublicEmailWhenTheEmailApiIsUnavailable() {
        ExternalProfile profile =
            GitHubProfileClient.profile(Map.of("id", 7, "login", "octocat", "email", "public@example.com"), null);

        assertEquals("7", profile.providerUserId());
        assertEquals("public@example.com", profile.email());
        assertTrue(profile.emailVerified());
    }

    @Test
    void rejectsAMissingUserId() {
        assertThrows(OAuthException.class, () -> GitHubProfileClient.profile(Map.of("login", "octocat"), null));
    }

    @Test
    void rejectsABlankAccessToken() {
        GitHubProfileClient client = new GitHubProfileClient(
            new OAuthOutboundClient(), new ObjectMapper(), "http://127.0.0.1/user", "http://127.0.0.1/emails");
        assertThrows(OAuthException.class, () -> client.load(" "));
    }

    @Test
    void skipsUnverifiedAddresses() {
        assertEquals(
            "verified@example.com",
            GitHubProfileClient.verifiedEmail(List.of(
                Map.of("email", "nope@example.com", "verified", false, "primary", true),
                Map.of("email", "verified@example.com", "verified", true, "primary", false))));
        assertFalse(GitHubProfileClient.profile(Map.of("id", 1, "login", "octocat"), null)
            .emailVerified());
    }
}
