package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.PasswordResetConfirmRequest;
import com.posadskiy.auth.api.dto.PasswordResetRequest;
import com.posadskiy.auth.api.dto.UserResponse;
import com.posadskiy.auth.core.crypto.EmailNormalizer;
import com.posadskiy.auth.core.service.ActionTokenService;
import com.posadskiy.auth.core.service.TokenService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.UsersRepository;
import com.posadskiy.auth.core.storage.db.entity.ActionTokenEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;
import jakarta.validation.Valid;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/account")
public class AccountController {

    private final ActionTokenService actionTokenService;
    private final UserAccountService userAccountService;
    private final UsersRepository usersRepository;
    private final TokenService tokenService;
    private final SecurityService securityService;

    public AccountController(
            ActionTokenService actionTokenService,
            UserAccountService userAccountService,
            UsersRepository usersRepository,
            TokenService tokenService,
            SecurityService securityService) {
        this.actionTokenService = actionTokenService;
        this.userAccountService = userAccountService;
        this.usersRepository = usersRepository;
        this.tokenService = tokenService;
        this.securityService = securityService;
    }

    @Get("/me")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public UserResponse me() {
        UserEntity user = userAccountService.requireActive(currentUserId());
        return AuthController.toResponse(user);
    }

    @Get("/verify")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> verify(@QueryValue String token) {
        ActionTokenEntity action = actionTokenService.consume(token, ActionTokenService.VERIFY_EMAIL);
        usersRepository.findById(action.getUserId()).ifPresent(userAccountService::markVerified);
        return HttpResponse.noContent();
    }

    @Post("/forgot-password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> forgot(@Body @Valid PasswordResetRequest request) {
        usersRepository
                .findByEmailNormalized(EmailNormalizer.normalize(request.email()))
                .ifPresent(actionTokenService::sendPasswordReset);
        return HttpResponse.accepted();
    }

    @Post("/reset-password")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> reset(@Body @Valid PasswordResetConfirmRequest request) {
        ActionTokenEntity action = actionTokenService.consume(request.token(), ActionTokenService.RESET_PASSWORD);
        UserEntity user = userAccountService.requireActive(action.getUserId());
        userAccountService.changePassword(user, request.password());
        tokenService.revokeAll(user.getId());
        return HttpResponse.noContent();
    }

    @Post("/deactivate")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> deactivate() {
        UserEntity user = userAccountService.requireActive(currentUserId());
        userAccountService.deactivate(user);
        tokenService.revokeAll(user.getId());
        return HttpResponse.noContent();
    }

    private long currentUserId() {
        return Long.parseLong(securityService
                .username()
                .orElseThrow(() -> new com.posadskiy.auth.core.exception.InvalidCredentialsException()));
    }
}
