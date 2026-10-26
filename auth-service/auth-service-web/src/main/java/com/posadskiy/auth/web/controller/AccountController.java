package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.UserResponse;
import com.posadskiy.auth.core.service.TokenService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/account")
public class AccountController {

    private final UserAccountService userAccountService;
    private final TokenService tokenService;
    private final SecurityService securityService;

    public AccountController(
        UserAccountService userAccountService, TokenService tokenService, SecurityService securityService) {
        this.userAccountService = userAccountService;
        this.tokenService = tokenService;
        this.securityService = securityService;
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

    private long currentUserId() {
        return Long.parseLong(securityService
            .username()
            .orElseThrow(() -> new com.posadskiy.auth.core.exception.InvalidCredentialsException()));
    }
}
