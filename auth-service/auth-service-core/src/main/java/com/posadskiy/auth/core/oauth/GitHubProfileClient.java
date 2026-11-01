package com.posadskiy.auth.core.oauth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posadskiy.auth.core.exception.OAuthException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * GitHub's OAuth token response has an access token and no ID token. The account id and verified
 * email come from the user API.
 */
@Singleton
public class GitHubProfileClient {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubProfileClient.class);
    static final String USER_URI = "https://api.github.com/user";
    static final String EMAILS_URI = "https://api.github.com/user/emails";

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> LIST = new TypeReference<>() {};

    private final OAuthOutboundClient http;
    private final ObjectMapper json;
    private final String userUri;
    private final String emailsUri;

    @Inject
    public GitHubProfileClient(OAuthOutboundClient http) {
        this(http, new ObjectMapper(), USER_URI, EMAILS_URI);
    }

    GitHubProfileClient(OAuthOutboundClient http, ObjectMapper json, String userUri, String emailsUri) {
        this.http = http;
        this.json = json;
        this.userUri = userUri;
        this.emailsUri = emailsUri;
    }

    public ExternalProfile load(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new OAuthException("GitHub did not return an access token");
        }
        Map<String, String> headers = Map.of(
            "Authorization", "Bearer " + accessToken,
            "Accept", "application/vnd.github+json",
            "User-Agent", "auth-service",
            "X-GitHub-Api-Version", "2022-11-28");
        Map<String, Object> user;
        try {
            user = json.readValue(http.get(userUri, headers), MAP);
        } catch (OAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuthException("GitHub profile could not be read");
        }
        String verifiedEmail = null;
        try {
            verifiedEmail = verifiedEmail(json.readValue(http.get(emailsUri, headers), LIST));
        } catch (Exception e) {
            LOG.debug("GitHub email list unavailable", e);
            verifiedEmail = null;
        }
        return profile(user, verifiedEmail);
    }

    static ExternalProfile profile(Map<String, Object> user, String verifiedEmail) {
        String email = verifiedEmail;
        boolean emailVerified = verifiedEmail != null;
        if (email == null) {
            email = text(user.get("email"));
            emailVerified = email != null;
        }
        String name = text(user.get("name"));
        if (name == null) {
            name = text(user.get("login"));
        }
        return new ExternalProfile("github", userId(user), email, emailVerified, name, text(user.get("avatar_url")));
    }

    static String verifiedEmail(List<Map<String, Object>> emails) {
        String fallback = null;
        if (emails == null) {
            return null;
        }
        for (Map<String, Object> email : emails) {
            String address = text(email.get("email"));
            if (address == null || !truth(email.get("verified"))) {
                continue;
            }
            if (truth(email.get("primary"))) {
                return address;
            }
            if (fallback == null) {
                fallback = address;
            }
        }
        return fallback;
    }

    private static String userId(Map<String, Object> user) {
        Object id = user.get("id");
        if (id instanceof Number number) {
            return Long.toString(number.longValue());
        }
        String text = text(id);
        if (text == null) {
            throw new OAuthException("GitHub user id is missing");
        }
        return text;
    }

    private static boolean truth(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value == null ? "false" : value));
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
