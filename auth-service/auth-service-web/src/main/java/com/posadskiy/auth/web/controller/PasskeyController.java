package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.service.AuthMetrics;
import com.posadskiy.auth.core.service.PasskeyService;
import com.posadskiy.auth.core.service.TokenService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import com.posadskiy.auth.web.ClientContextFactory;
import com.posadskiy.auth.web.RefreshSession;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;

import java.io.IOException;
import java.util.Map;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller
public class PasskeyController {

    private final PasskeyService passkeys;
    private final UserAccountService userAccountService;
    private final TokenService tokenService;
    private final AuthMetrics metrics;
    private final SecurityService securityService;
    private final JsonMapper json;
    private final RefreshSession refreshSession;

    public PasskeyController(
        PasskeyService passkeys,
        UserAccountService userAccountService,
        TokenService tokenService,
        AuthMetrics metrics,
        SecurityService securityService,
        JsonMapper json,
        RefreshSession refreshSession) {
        this.passkeys = passkeys;
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
        this.metrics = metrics;
        this.securityService = securityService;
        this.json = json;
        this.refreshSession = refreshSession;
    }

    private static String origin(HttpRequest<?> request, JsonNode root) {
        String header = request.getHeaders().get("Origin");
        if (header != null && !header.isBlank()) {
            return header;
        }
        return text(root, "linkOrigin");
    }

    private static String text(JsonNode root, String field) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode value = root.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.getStringValue();
    }

    @Post("/account/passkeys/options")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public Map<String, Object> registrationOptions(HttpRequest<?> request) {
        return ceremony(passkeys.registrationOptions(currentUser(), origin(request, null)));
    }

    @Post(uri = "/account/passkeys", consumes = MediaType.APPLICATION_JSON)
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> register(@Body String body) {
        JsonNode root = read(body);
        String label = text(root, "label");
        passkeys.finishRegistration(currentUser(), root.get("challengeId").getLongValue(), credential(root), label);
        return HttpResponse.noContent();
    }

    @Post("/login/passkey/options")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public Map<String, Object> loginOptions(@Body String body, HttpRequest<?> request) {
        JsonNode root = body == null || body.isBlank() ? JsonNode.nullNode() : read(body);
        return ceremony(passkeys.loginOptions(text(root, "email"), origin(request, root)));
    }

    @Post(uri = "/login/passkey", consumes = MediaType.APPLICATION_JSON)
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<TokenResponse> login(@Body String body, HttpRequest<?> request) {
        JsonNode root = read(body);
        UserEntity user = passkeys.finishLogin(root.get("challengeId").getLongValue(), credential(root));
        metrics.loginSuccess("passkey");
        return refreshSession.respond(
            request,
            tokenService.issueForUser(user, "passkey", null, ClientContextFactory.from(request, text(root, "linkOrigin"))));
    }

    private Map<String, Object> ceremony(PasskeyService.PasskeyCeremony ceremony) {
        try {
            Object parsed = json.readValue(ceremony.publicKeyJson().getBytes(), Argument.OBJECT_ARGUMENT);
            return Map.of("challengeId", ceremony.challengeId(), "publicKey", options(parsed));
        } catch (IOException e) {
            throw new AuthException("Passkey options could not be read");
        }
    }

    /**
     * Yubico's credentials-create and credentials-get JSON is {@code {"publicKey": options}}.
     * The browser ceremony needs the inner options, with {@code challenge} at the top.
     */
    private static Object options(Object parsed) {
        if (parsed instanceof Map<?, ?> wrapped && wrapped.get("publicKey") instanceof Map<?, ?> inner) {
            return inner;
        }
        return parsed;
    }

    private JsonNode read(String body) {
        try {
            return json.readValue(body.getBytes(), Argument.of(JsonNode.class));
        } catch (IOException e) {
            throw new AuthException("Passkey response is missing");
        }
    }

    private String credential(JsonNode root) {
        JsonNode credential = root.get("credential");
        if (credential == null || credential.isNull()) {
            throw new AuthException("Passkey response is missing");
        }
        try {
            return json.writeValueAsString(credential);
        } catch (IOException e) {
            throw new AuthException("Passkey response is missing");
        }
    }

    private UserEntity currentUser() {
        long userId = Long.parseLong(securityService
            .username()
            .orElseThrow(() -> new com.posadskiy.auth.core.exception.InvalidCredentialsException()));
        return userAccountService.requireActive(userId);
    }
}
