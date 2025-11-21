package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.oauth.JwtTokenService;
import com.posadskiy.auth.core.oauth.JwtTokenService.TokenDetails;
import com.posadskiy.auth.core.oauth.OAuthAuthorizationService;
import com.posadskiy.auth.core.oauth.OAuthAuthorizationService.AuthorizationRequest;
import com.posadskiy.auth.core.oauth.OAuthTokenExchangeService;
import com.posadskiy.auth.core.service.RefreshTokenIssuer;
import com.posadskiy.auth.core.service.SocialLoginService;
import com.posadskiy.auth.core.service.SocialLoginService.SocialLoginResult;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/oauth2")
public class OAuthController {

    private final OAuthAuthorizationService authorizationService;
    private final OAuthTokenExchangeService tokenExchangeService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenIssuer refreshTokenIssuer;

    public OAuthController(
            OAuthAuthorizationService authorizationService,
            OAuthTokenExchangeService tokenExchangeService,
            SocialLoginService socialLoginService,
            JwtTokenService jwtTokenService,
            RefreshTokenIssuer refreshTokenIssuer) {
        this.authorizationService = authorizationService;
        this.tokenExchangeService = tokenExchangeService;
        this.socialLoginService = socialLoginService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenIssuer = refreshTokenIssuer;
    }

    @Get("/authorize/{provider}")
    public HttpResponse<AuthorizationRedirectResponse> authorize(
            @PathVariable @NotBlank String provider) {
        AuthorizationRequest request = authorizationService.authorize(provider, "");
        return HttpResponse.ok(new AuthorizationRedirectResponse(request.authorizationUri(), request.state(), request.nonce()));
    }

    @Get("/callback/{provider}")
    public HttpResponse<OAuthTokenResponse> callback(
            @PathVariable @NotBlank String provider,
            @QueryValue("code") String code,
            @QueryValue("state") String state) {
        ExternalProfile profile = tokenExchangeService.exchange(provider, code, state);
        SocialLoginResult loginResult = socialLoginService.handle(profile);
        TokenDetails tokenDetails = jwtTokenService.generate(loginResult.user(), loginResult.identity());
        String refreshToken =
                refreshTokenIssuer.issue(loginResult.user().getId(), provider, loginResult.identity().getId());

        OAuthTokenResponse response =
                new OAuthTokenResponse(
                        tokenDetails.accessToken(),
                        refreshToken,
                        tokenDetails.expiresInSeconds(),
                        "Bearer",
                        loginResult.user().getId(),
                        loginResult.identity().getProvider(),
                        loginResult.identity().getProviderUserId());
        return HttpResponse.ok(response);
    }

    @Serdeable
    public record AuthorizationRedirectResponse(String authorizationUri, String state, String nonce) {}

    @Serdeable
    public record OAuthTokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String tokenType,
            Long userId,
            String provider,
            String providerUserId) {}
}

