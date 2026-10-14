package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.api.dto.OAuthFinalizeRequest;
import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.crypto.SecureTokens;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.service.AuthMetrics;
import com.posadskiy.auth.core.service.OAuthAuthorizationService;
import com.posadskiy.auth.core.service.OAuthAuthorizationService.AuthorizationRequest;
import com.posadskiy.auth.core.service.OAuthLoginCodeService;
import com.posadskiy.auth.core.service.OAuthTokenExchangeService;
import com.posadskiy.auth.core.service.OAuthTokenExchangeService.ExchangeResult;
import com.posadskiy.auth.core.service.SocialLoginService;
import com.posadskiy.auth.core.service.SocialLoginService.SocialLoginResult;
import com.posadskiy.auth.core.service.TokenService;
import com.posadskiy.auth.core.service.UserAccountService;
import com.posadskiy.auth.core.storage.db.ExternalIdentityRepository;
import com.posadskiy.auth.core.storage.db.entity.OAuthLoginCodeEntity;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.CookieValue;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.cookie.SameSite;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/oauth2")
public class OAuthController {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthController.class);
    private static final String BROWSER_COOKIE = "oauth_browser";

    private final OAuthAuthorizationService authorizationService;
    private final OAuthTokenExchangeService tokenExchangeService;
    private final SocialLoginService socialLoginService;
    private final TokenService tokenService;
    private final OAuthLoginCodeService loginCodeService;
    private final UserAccountService userAccountService;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final AuthProperties authProperties;
    private final AuthMetrics metrics;

    public OAuthController(
            OAuthAuthorizationService authorizationService,
            OAuthTokenExchangeService tokenExchangeService,
            SocialLoginService socialLoginService,
            TokenService tokenService,
            OAuthLoginCodeService loginCodeService,
            UserAccountService userAccountService,
            ExternalIdentityRepository externalIdentityRepository,
            AuthProperties authProperties,
            AuthMetrics metrics) {
        this.authorizationService = authorizationService;
        this.tokenExchangeService = tokenExchangeService;
        this.socialLoginService = socialLoginService;
        this.tokenService = tokenService;
        this.loginCodeService = loginCodeService;
        this.userAccountService = userAccountService;
        this.externalIdentityRepository = externalIdentityRepository;
        this.authProperties = authProperties;
        this.metrics = metrics;
    }

    @Get("/authorize/{provider}")
    public HttpResponse<AuthorizationRedirectResponse> authorize(
            @PathVariable @NotBlank String provider,
            @QueryValue(value = "code_challenge", defaultValue = "") String codeChallenge,
            HttpRequest<?> request) {
        String binding = existingOrNewBinding(request);
        AuthorizationRequest authorization =
                authorizationService.authorize(provider, binding, blankToNull(codeChallenge));
        MutableHttpResponse<AuthorizationRedirectResponse> response = HttpResponse.ok(
                new AuthorizationRedirectResponse(authorization.authorizationUri(), authorization.state()));
        response.cookie(browserCookie(binding, request.isSecure()));
        return response;
    }

    @Get("/callback/{provider}")
    public HttpResponse<?> callbackGet(
            @PathVariable String provider,
            @QueryValue("code") String code,
            @QueryValue(value = "state", defaultValue = "") String state,
            @CookieValue(value = BROWSER_COOKIE, defaultValue = "") String binding) {
        return complete(provider, code, state, binding, null);
    }

    @Post("/callback/{provider}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> callbackPost(
            @PathVariable String provider,
            @QueryValue(value = "code", defaultValue = "") String code,
            @QueryValue(value = "state", defaultValue = "") String state,
            @QueryValue(value = "user", defaultValue = "") String user,
            @CookieValue(value = BROWSER_COOKIE, defaultValue = "") String binding,
            @Nullable @Body CallbackForm form) {
        String resolvedCode = (form != null && form.code() != null) ? form.code() : code;
        String resolvedState = (form != null && form.state() != null) ? form.state() : state;
        String resolvedUser = (form != null && form.user() != null) ? form.user() : user;
        return complete(provider, resolvedCode, resolvedState, binding, blankToNull(resolvedUser));
    }

    @Post("/finalize")
    public TokenResponse finalizeLogin(@Body @Valid OAuthFinalizeRequest request) {
        OAuthLoginCodeEntity loginCode = loginCodeService.consume(request.code(), request.codeVerifier());
        var user = userAccountService.requireActive(loginCode.userId());
        String provider = loginCode.externalIdentityId() == null
                ? null
                : externalIdentityRepository
                        .findById(loginCode.externalIdentityId())
                        .map(identity -> identity.provider())
                        .orElse(null);
        metrics.loginSuccess("oauth");
        return tokenService.issueForUser(user, provider, loginCode.externalIdentityId());
    }

    private HttpResponse<?> complete(String provider, String code, String state, String binding, String appleUserJson) {
        if (code == null || code.isBlank() || state == null || state.isBlank()) {
            return HttpResponse.badRequest();
        }
        try {
            ExchangeResult exchange = tokenExchangeService.exchange(provider, code, state, binding, appleUserJson);
            ExternalProfile profile = exchange.profile();
            SocialLoginResult login = socialLoginService.handle(profile);
            String frontend = authProperties.oauth().frontendRedirectUrl();
            if (frontend != null && !frontend.isBlank()) {
                String sessionCode =
                        loginCodeService.create(login.user(), login.identity().id(), exchange.codeChallenge());
                URI redirect =
                        UriBuilder.of(frontend).queryParam("code", sessionCode).build();
                metrics.oauth(provider, "success");
                return HttpResponse.redirect(redirect);
            }
            metrics.oauth(provider, "success");
            TokenResponse tokens = tokenService.issueForUser(
                    login.user(), login.identity().provider(), login.identity().id());
            MutableHttpResponse<TokenResponse> response = HttpResponse.ok(tokens);
            response.header("Cache-Control", "no-store");
            return response;
        } catch (OAuthException e) {
            LOG.warn("OAuth callback failed provider={} status={}", provider, e.status());
            metrics.oauth(provider, "failure");
            throw e;
        }
    }

    private String existingOrNewBinding(HttpRequest<?> request) {
        return request.getCookies()
                .findCookie(BROWSER_COOKIE)
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .orElseGet(() -> SecureTokens.randomUrl(32));
    }

    private Cookie browserCookie(String binding, boolean secure) {
        Cookie cookie = Cookie.of(BROWSER_COOKIE, binding)
                .httpOnly(true)
                .path("/oauth2")
                .maxAge(Duration.ofMinutes(10))
                .sameSite(SameSite.Lax);
        if (secure) {
            cookie.secure(true);
        }
        return cookie;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Serdeable
    public record AuthorizationRedirectResponse(String authorizationUri, String state) {}

    @Serdeable
    public record CallbackForm(String code, String state, String user) {}
}
