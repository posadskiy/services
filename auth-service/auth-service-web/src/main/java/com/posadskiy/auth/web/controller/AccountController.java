package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.service.*;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import com.posadskiy.auth.web.ClientAddresses;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;
import jakarta.validation.Valid;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/account")
public class AccountController {

    private final UserAccountService userAccountService;
    private final TokenService tokenService;
    private final AccountMailService accountMailService;
    private final LoginRateLimiter rateLimiter;
    private final SecurityService securityService;
    private final CaptchaVerifier captchaVerifier;

    public AccountController(
        UserAccountService userAccountService,
        TokenService tokenService,
        AccountMailService accountMailService,
        LoginRateLimiter rateLimiter,
        SecurityService securityService,
        CaptchaVerifier captchaVerifier) {
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
        this.accountMailService = accountMailService;
        this.rateLimiter = rateLimiter;
        this.securityService = securityService;
        this.captchaVerifier = captchaVerifier;
    }

    private static String clientKey(HttpRequest<?> request) {
        return ClientAddresses.key(request);
    }

    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public UserResponse me() {
        UserEntity user = userAccountService.requireActive(currentUserId());
        return AuthController.toResponse(user);
    }

    @Post("/deactivate")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> deactivate() {
        UserEntity user = userAccountService.requireActive(currentUserId());
        userAccountService.deactivate(user);
        tokenService.revokeAll(user.id());
        return HttpResponse.noContent();
    }

    @Post("/verify")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> verify(@Body @Valid VerifyEmailRequest request) {
        accountMailService.verifyEmail(request.token());
        return HttpResponse.noContent();
    }

    @Post("/verification-email")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> verificationEmail(@Body @Valid LinkOriginRequest request, HttpRequest<?> httpRequest) {
        UserEntity user = userAccountService.requireActive(currentUserId());
        rateLimiter.check(clientKey(httpRequest) + "|verify|" + user.id());
        accountMailService.sendVerification(user, request.linkOrigin());
        return HttpResponse.noContent();
    }

    @Post("/forgot-password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> forgotPassword(@Body @Valid ForgotPasswordRequest request, HttpRequest<?> httpRequest) {
        String email = EmailNormalizer.normalize(request.email());
        captchaVerifier.verify(request.captchaToken(), clientKey(httpRequest));
        rateLimiter.checkAddress(clientKey(httpRequest));
        rateLimiter.check(clientKey(httpRequest) + "|forgot|" + email);
        accountMailService.sendPasswordReset(request.email(), request.linkOrigin());
        return HttpResponse.noContent();
    }

    @Post("/reset-password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> resetPassword(@Body @Valid ResetPasswordRequest request) {
        accountMailService.resetPassword(request.token(), request.password());
        return HttpResponse.noContent();
    }

    private long currentUserId() {
        return Long.parseLong(securityService
            .username()
            .orElseThrow(() -> new com.posadskiy.auth.core.exception.InvalidCredentialsException()));
    }
}
