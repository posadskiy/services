package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.AppCodeRequest;
import com.posadskiy.auth.api.dto.AppCodeResponse;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import com.posadskiy.auth.core.service.AccountMailService;
import com.posadskiy.auth.core.service.OAuthLoginCodeService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.RefreshTokenRepository;
import com.posadskiy.auth.core.storage.db.entity.RefreshTokenEntity;
import com.posadskiy.auth.web.RefreshSession;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;

import java.time.Duration;
import java.time.Instant;

/**
 * Lets a signed-in browser sign in a native app on the same device.
 *
 * <p>The browser cannot pass on its refresh token (an httpOnly cookie), so it asks for a one-time code bound to the
 * app's PKCE challenge. The app redeems the code at {@code POST /oauth2/finalize} with its verifier and gets its own
 * refresh-token family.
 *
 * <p>Minting a long-lived session is guarded more tightly than an ordinary API call:
 * <ul>
 *   <li>The credential is the refresh cookie, not a bearer token. A leaked access token (a log line, a backend that
 *       received it) cannot be turned into a lasting session.</li>
 *   <li>The request must come from a browser, from an allowlisted site ({@code Origin}).</li>
 *   <li>The browser sign-in must be recent. An old or stolen browser session has to sign in again first.</li>
 * </ul>
 */
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/account")
public class AppHandoffController {

    /** How long after a sign-in the browser may hand it to an app without signing in again. */
    static final Duration MAX_AUTH_AGE = Duration.ofMinutes(10);

    private final RefreshTokenRepository refreshTokens;
    private final UserAccountService users;
    private final AccountMailService mail;
    private final OAuthLoginCodeService loginCodes;

    public AppHandoffController(
        RefreshTokenRepository refreshTokens,
        UserAccountService users,
        AccountMailService mail,
        OAuthLoginCodeService loginCodes) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.mail = mail;
        this.loginCodes = loginCodes;
    }

    @Post("/app-code")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<AppCodeResponse> appCode(@Body @Valid AppCodeRequest body, HttpRequest<?> request) {
        if (!RefreshSession.isBrowser(request)) {
            throw new AuthException("App handoff starts in the browser");
        }
        String origin = request.getHeaders().getOrigin().orElse(null);
        mail.checkOrigin(origin);

        String cookie = RefreshSession.cookie(request);
        if (cookie == null) {
            throw new InvalidCredentialsException();
        }
        Instant now = Instant.now();
        RefreshTokenEntity current = refreshTokens
            .findByTokenHash(SecureTokens.sha256Hex(cookie))
            .filter(token -> !token.revoked() && token.replacedById() == null && token.expiresAt().isAfter(now))
            .orElseThrow(InvalidCredentialsException::new);

        Instant signedInAt = refreshTokens
            .findFirstByFamilyIdOrderByCreatedAtAsc(current.familyId())
            .map(RefreshTokenEntity::createdAt)
            .orElse(null);
        if (signedInAt == null || signedInAt.isBefore(now.minus(MAX_AUTH_AGE))) {
            throw new AuthException("Sign in again to connect the app", 403);
        }

        var user = users.requireActive(current.userId());
        String code = loginCodes.create(user, current.externalIdentityId(), body.codeChallenge(), origin);
        return HttpResponse.ok(new AppCodeResponse(code)).header("Cache-Control", "no-store");
    }
}
