package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.service.AccountMailService;
import com.posadskiy.auth.core.service.AccountMaintenanceService;
import com.posadskiy.auth.core.service.PasskeyService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.entity.PasskeyCredentialEntity;
import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import com.posadskiy.auth.core.storage.db.entity.UserEntity;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/account")
public class AccountSecurityController {

    private final UserAccountService userAccountService;
    private final AccountMaintenanceService maintenance;
    private final AccountMailService accountMailService;
    private final PasskeyService passkeys;
    private final SecurityService securityService;

    public AccountSecurityController(
        UserAccountService userAccountService,
        AccountMaintenanceService maintenance,
        AccountMailService accountMailService,
        PasskeyService passkeys,
        SecurityService securityService) {
        this.userAccountService = userAccountService;
        this.maintenance = maintenance;
        this.accountMailService = accountMailService;
        this.passkeys = passkeys;
        this.securityService = securityService;
    }

    @Get("/sessions")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<SessionResponse> sessions() {
        return maintenance.sessions(currentUser().id()).stream().map(this::session).toList();
    }

    @Delete("/sessions/{id}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> revokeSession(@PathVariable String id) {
        maintenance.revokeSession(currentUser().id(), UUID.fromString(id));
        return HttpResponse.noContent();
    }

    @Post("/password")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> changePassword(@Body @Valid ChangePasswordRequest request) {
        maintenance.changePassword(currentUser(), request.currentPassword(), request.password());
        return HttpResponse.noContent();
    }

    @Post("/password/set")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> setPassword(@Body @Valid SetPasswordRequest request) {
        maintenance.setPassword(currentUser(), request.password());
        return HttpResponse.noContent();
    }

    @Post("/email")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> changeEmail(@Body @Valid ChangeEmailRequest request) {
        UserEntity user = currentUser();
        accountMailService.requestEmailChange(user, request.email(), request.linkOrigin(), request.password());
        return HttpResponse.noContent();
    }

    @Post("/email/confirm")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<Void> confirmEmail(@Body @Valid ConfirmEmailRequest request) {
        accountMailService.confirmEmailChange(request.token());
        return HttpResponse.noContent();
    }

    @Get("/identities")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<IdentityResponse> identities() {
        return maintenance.identities(currentUser().id()).stream()
            .map(identity -> new IdentityResponse(identity.provider(), identity.email()))
            .toList();
    }

    @Delete("/identities/{provider}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> unlink(@PathVariable String provider) {
        maintenance.unlink(currentUser(), provider);
        return HttpResponse.noContent();
    }

    @Get("/passkeys")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public List<PasskeyResponse> passkeys() {
        return passkeys.list(currentUser().id()).stream().map(this::passkey).toList();
    }

    @Delete("/passkeys/{id}")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> deletePasskey(@PathVariable String id) {
        UserEntity user = currentUser();
        long passkeyId = Long.parseLong(id);
        passkeys.ensureCanRemove(
            user, passkeys.count(user.id()) - 1, maintenance.identities(user.id()).size());
        passkeys.delete(user, passkeyId);
        return HttpResponse.noContent();
    }

    @Get("/export")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public Map<String, Object> export() {
        return maintenance.export(currentUser());
    }

    @Post("/erase")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> erase(@Body @Valid EraseAccountRequest request) {
        maintenance.erase(currentUser(), request.password(), request.confirmation());
        return HttpResponse.noContent();
    }

    private SessionResponse session(RefreshTokenEntity token) {
        return new SessionResponse(
            token.familyId().toString(),
            token.provider(),
            token.userAgent(),
            token.clientIp(),
            token.createdAt() == null ? null : token.createdAt().toString());
    }

    private PasskeyResponse passkey(PasskeyCredentialEntity credential) {
        return new PasskeyResponse(
            String.valueOf(credential.id()),
            credential.label(),
            credential.createdAt() == null ? null : credential.createdAt().toString(),
            credential.lastUsedAt() == null ? null : credential.lastUsedAt().toString());
    }

    private UserEntity currentUser() {
        long userId = Long.parseLong(securityService
            .username()
            .orElseThrow(() -> new com.posadskiy.auth.core.exception.InvalidCredentialsException()));
        return userAccountService.requireActive(userId);
    }
}
