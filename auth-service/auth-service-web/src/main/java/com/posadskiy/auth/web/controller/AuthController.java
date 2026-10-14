package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.LoginRequest;
import com.posadskiy.auth.api.dto.RefreshRequest;
import com.posadskiy.auth.api.dto.RegisterRequest;
import com.posadskiy.auth.api.dto.RevokeRequest;
import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.api.dto.UserResponse;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.service.ActionTokenService;
import com.posadskiy.auth.core.service.AuditService;
import com.posadskiy.auth.core.service.AuthMetrics;
import com.posadskiy.auth.core.service.LoginRateLimiter;
import com.posadskiy.auth.core.service.TokenService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller
public class AuthController {

    private final UserAccountService userAccountService;
    private final TokenService tokenService;
    private final ActionTokenService actionTokenService;
    private final LoginRateLimiter rateLimiter;
    private final AuthMetrics metrics;
    private final AuditService auditService;

    public AuthController(
            UserAccountService userAccountService,
            TokenService tokenService,
            ActionTokenService actionTokenService,
            LoginRateLimiter rateLimiter,
            AuthMetrics metrics,
            AuditService auditService) {
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
        this.actionTokenService = actionTokenService;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.auditService = auditService;
    }

    @Post("/users")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserResponse> register(@Body @Valid RegisterRequest request) {
        UserEntity user = userAccountService.register(request.email(), request.password(), request.username());
        actionTokenService.sendVerifyEmail(user);
        return HttpResponse.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public TokenResponse login(@Body @Valid LoginRequest request, HttpRequest<?> httpRequest) {
        rateLimiter.check(clientKey(httpRequest) + "|login|" + request.username());
        try {
            UserEntity user = userAccountService.authenticate(request.username(), request.password());
            metrics.loginSuccess("password");
            auditService.record(user.getId(), "LOGIN_SUCCESS", "password");
            return tokenService.issueForUser(user, "password", null);
        } catch (AuthException e) {
            metrics.loginFailure("password");
            throw e;
        }
    }

    @Post("/oauth/token")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public TokenResponse token(@Body @Valid RefreshRequest request, HttpRequest<?> httpRequest) {
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

    private static String clientKey(HttpRequest<?> request) {
        return request.getRemoteAddress().getAddress().getHostAddress();
    }

    static UserResponse toResponse(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getPictureUrl(),
                user.roleList());
    }
}
