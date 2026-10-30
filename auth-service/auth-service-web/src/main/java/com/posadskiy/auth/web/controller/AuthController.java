package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import com.posadskiy.auth.core.service.*;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import com.posadskiy.auth.web.ClientAddresses;
import com.posadskiy.auth.web.ClientContextFactory;
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
    private final AccountMailService accountMailService;
    private final CaptchaVerifier captchaVerifier;

    public AuthController(
        UserAccountService userAccountService,
        TokenService tokenService,
        LoginRateLimiter rateLimiter,
        AuthMetrics metrics,
        AuditService auditService,
        AccountMailService accountMailService,
        CaptchaVerifier captchaVerifier) {
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
        this.rateLimiter = rateLimiter;
        this.metrics = metrics;
        this.auditService = auditService;
        this.accountMailService = accountMailService;
        this.captchaVerifier = captchaVerifier;
    }

    private static String clientKey(HttpRequest<?> request) {
        return ClientAddresses.key(request);
    }

    /**
     * Traefik adds {@code X-Forwarded-For} to everything it forwards; pods calling the service directly do not.
     */
    static boolean viaIngress(HttpRequest<?> request) {
        return request.getHeaders().get("X-Forwarded-For") != null
            || request.getHeaders().get("Forwarded") != null
            || request.getHeaders().get("X-Real-Ip") != null;
    }

    static UserResponse toResponse(UserEntity user) {
        return new UserResponse(
            UserResponse.idOf(user.id()), user.email(), user.emailVerified(), user.pictureUrl(), user.roleList());
    }

    /**
     * Wrong passwords are counted per account across all addresses, so a botnet spreading one account over many
     * addresses still ends up facing the captcha.
     */
    private void challengeIfNeeded(String email, String captchaToken, HttpRequest<?> httpRequest) {
        String account = EmailNormalizer.normalize(email);
        if (account != null && rateLimiter.challengeRequired(account)) {
            captchaVerifier.verify(captchaToken, clientKey(httpRequest));
        }
    }

    private UserEntity authenticate(String email, String password) {
        String account = EmailNormalizer.normalize(email);
        try {
            UserEntity user = userAccountService.authenticate(email, password);
            if (account != null) {
                rateLimiter.clearFailures(account);
            }
            return user;
        } catch (InvalidCredentialsException e) {
            if (account != null) {
                rateLimiter.recordFailure(account);
            }
            throw e;
        }
    }

    private TokenResponse passwordSession(UserEntity user, String linkOrigin, HttpRequest<?> httpRequest) {
        if (!user.emailVerified()) {
            resendVerification(user, linkOrigin);
            throw new AuthException("Confirm your email before signing in", 403);
        }
        return tokenService.issueForUser(user, "password", null, ClientContextFactory.from(httpRequest, linkOrigin));
    }

    private void resendVerification(UserEntity user, String linkOrigin) {
        if (linkOrigin == null || linkOrigin.isBlank()) {
            return;
        }
        try {
            accountMailService.sendVerification(user, linkOrigin);
        } catch (AuthException ignored) {
            // A rejected origin still leaves the account unsigned-in.
        }
    }

    @Post(uris = {"/users", "/signup"})
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<UserResponse> register(@Body @Valid RegisterRequest request, HttpRequest<?> httpRequest) {
        captchaVerifier.verify(request.captchaToken(), clientKey(httpRequest));
        rateLimiter.check(clientKey(httpRequest) + "|register|" + request.email());
        accountMailService.checkOrigin(request.linkOrigin());
        UserEntity user = userAccountService.register(request.email(), request.password());
        accountMailService.sendVerification(user, request.linkOrigin());
        return HttpResponse.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @Post("/login")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public TokenResponse login(@Body @Valid LoginRequest request, HttpRequest<?> httpRequest) {
        rateLimiter.check(clientKey(httpRequest) + "|login|" + request.email());
        challengeIfNeeded(request.email(), request.captchaToken(), httpRequest);
        try {
            UserEntity user = authenticate(request.email(), request.password());
            TokenResponse tokens = passwordSession(user, request.linkOrigin(), httpRequest);
            metrics.loginSuccess("password");
            auditService.record(user.id(), "LOGIN_SUCCESS", "password");
            return tokens;
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
                form.get("email"),
                form.get("password"),
                form.get("captcha_token")),
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
                if (viaIngress(httpRequest)) {
                    // Machine clients live inside the cluster. A proxy header means the request came from outside.
                    throw new AuthException("Unsupported grant_type");
                }
                rateLimiter.check(clientKey(httpRequest) + "|client|" + request.clientId());
                yield tokenService.clientCredentials(request.clientId(), request.clientSecret());
            }
            case "password" -> {
                rateLimiter.check(clientKey(httpRequest) + "|login|" + request.email());
                challengeIfNeeded(request.email(), request.captchaToken(), httpRequest);
                UserEntity user = authenticate(request.email(), request.password());
                TokenResponse tokens = passwordSession(user, null, httpRequest);
                metrics.loginSuccess("password");
                yield tokens;
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
