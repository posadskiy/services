package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.service.*;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;

import java.util.Map;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller
public class AuthController {

    private final UserAccountService userAccountService;
    private final TokenService tokenService;
    private final LoginRateLimiter rateLimiter;
    private final AuthMetrics metrics;
    private final AuditService auditService;

    public AuthController(
        UserAccountService userAccountService,
        TokenService tokenService,
        LoginRateLimiter rateLimiter,
        AuthMetrics metrics,
        AuditService auditService) {
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.auditService = auditService;
    }

    private static String clientKey(HttpRequest<?> request) {
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    static UserResponse toResponse(UserEntity user) {
        return new UserResponse(
            UserResponse.idOf(user.id()),
            user.username(),
            user.email(),
            user.emailVerified(),
            user.pictureUrl(),
            user.roleList());
    }

    @Post(uris = {"/users", "/signup"})
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserResponse> register(@Body @Valid RegisterRequest request) {
        UserEntity user = userAccountService.register(request.email(), request.password(), request.username());
        return HttpResponse.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public TokenResponse login(@Body @Valid LoginRequest request, HttpRequest<?> httpRequest) {
        rateLimiter.check(clientKey(httpRequest) + "|login|" + request.username());
        try {
            UserEntity user = userAccountService.authenticate(request.username(), request.password());
            metrics.loginSuccess("password");
            auditService.record(user.id(), "LOGIN_SUCCESS", "password");
            return tokenService.issueForUser(user, "password", null);
        } catch (AuthException e) {
            metrics.loginFailure("password");
            throw e;
        }
    }

    @Post(uri = "/oauth/token", consumes = MediaType.APPLICATION_JSON)
    @Secured(SecurityRule.IS_ANONYMOUS)
    public TokenResponse token(@Body @Valid RefreshRequest request, HttpRequest<?> httpRequest) {
        return grant(request, httpRequest);
    }

    /**
     * OAuth 2.1 token requests are form-urlencoded. JSON stays accepted for clients already sending it.
     */
    @Post(uri = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    @Secured(SecurityRule.IS_ANONYMOUS)
    public TokenResponse tokenForm(@Body Map<String, String> form, HttpRequest<?> httpRequest) {
        return grant(
            new RefreshRequest(
                form.get("grant_type"),
                form.get("refresh_token"),
                form.get("client_id"),
                form.get("client_secret"),
                form.get("username"),
                form.get("password")),
            httpRequest);
    }

    private TokenResponse grant(RefreshRequest request, HttpRequest<?> httpRequest) {
        if (request.grantType() == null) {
            throw new AuthException("grant_type is required");
        }
        return switch (request.grantType()) {
            case "refresh_token" -> {
                if (request.refreshToken() == null) {
                    throw new AuthException("refresh_token is required");
                }
                yield tokenService.refresh(request.refreshToken());
            }
            case "client_credentials" -> {
                rateLimiter.check(clientKey(httpRequest) + "|client|" + request.clientId());
                yield tokenService.clientCredentials(request.clientId(), request.clientSecret());
            }
            case "password" -> {
                rateLimiter.check(clientKey(httpRequest) + "|login|" + request.username());
                UserEntity user = userAccountService.authenticate(request.username(), request.password());
                metrics.loginSuccess("password");
                yield tokenService.issueForUser(user, "password", null);
            }
            default -> throw new AuthException("Unsupported grant_type");
        };
    }

    @Post("/oauth/revoke")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> revoke(@Body @Valid RevokeRequest request) {
        tokenService.revoke(request.token());
        return HttpResponse.ok();
    }

    @Post("/logout")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> logout(@Body @Valid RevokeRequest request) {
        tokenService.revoke(request.token());
        return HttpResponse.ok();
    }
}
