package com.posadskiy.auth.web.controller;

import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.service.JwtTokenService;
import com.posadskiy.auth.core.service.JwtTokenService.TokenDetails;
import com.posadskiy.auth.core.service.OAuthAuthorizationService;
import com.posadskiy.auth.core.service.OAuthAuthorizationService.AuthorizationRequest;
import com.posadskiy.auth.core.service.OAuthTokenExchangeService;
import com.posadskiy.auth.core.service.RefreshTokenIssuer;
import com.posadskiy.auth.core.service.SocialLoginService;
import com.posadskiy.auth.core.service.SocialLoginService.SocialLoginResult;
import com.posadskiy.auth.core.property.SocialOAuthConfigurationProperties;
import com.posadskiy.auth.web.oauth.OAuthLoginSessionStore;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import io.micronaut.http.uri.UriBuilder;

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/oauth2")
public class OAuthController {

    private final OAuthAuthorizationService authorizationService;
    private final OAuthTokenExchangeService tokenExchangeService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final OAuthLoginSessionStore loginSessionStore;
    private final SocialOAuthConfigurationProperties socialOAuthConfigurationProperties;

    public OAuthController(
            OAuthAuthorizationService authorizationService,
            OAuthTokenExchangeService tokenExchangeService,
            SocialLoginService socialLoginService,
            JwtTokenService jwtTokenService,
            RefreshTokenIssuer refreshTokenIssuer,
            OAuthLoginSessionStore loginSessionStore,
            SocialOAuthConfigurationProperties socialOAuthConfigurationProperties) {
        this.authorizationService = authorizationService;
        this.tokenExchangeService = tokenExchangeService;
        this.socialLoginService = socialLoginService;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.loginSessionStore = loginSessionStore;
        this.socialOAuthConfigurationProperties = socialOAuthConfigurationProperties;
    }

    @Get("/authorize/{provider}")
    public HttpResponse<AuthorizationRedirectResponse> authorize(
            @PathVariable @NotBlank String provider) {
        AuthorizationRequest request = authorizationService.authorize(provider, "");
        
        // Log the authorization URI for debugging (contains redirect_uri parameter)
        System.out.println("OAuth Authorization Request for provider: " + provider);
        System.out.println("Authorization URI: " + request.authorizationUri());
        System.out.println("State: " + request.state());
        System.out.println("Redirect Base URL: " + socialOAuthConfigurationProperties.getRedirectBaseUrl());
        
        return HttpResponse.ok(new AuthorizationRedirectResponse(request.authorizationUri(), request.state(), request.nonce()));
    }

    // Single callback handler; provider path segment is ignored because provider is derived from state
    @Get("/callback/{providerPath:.+}")
    public HttpResponse<?> callback(
            @PathVariable("providerPath") String providerPath,
            @QueryValue("code") String code,
            @QueryValue(value = "state", defaultValue = "") String state) {
        
        // Log callback details for debugging
        System.out.println("=== OAuth Callback Received ===");
        System.out.println("Provider Path: " + providerPath);
        System.out.println("Code: " + (code != null ? "present" : "missing"));
        System.out.println("State: " + (StringUtils.isNotEmpty(state) ? state : "MISSING"));
        System.out.println("Expected redirect URI: http://localhost:8100/oauth2/callback/google");
        System.out.println("================================");
        
        // Validate required parameters
        if (StringUtils.isEmpty(code)) {
            return HttpResponse.badRequest("Missing required parameter: code");
        }
        if (StringUtils.isEmpty(state)) {
            String errorMessage = String.format(
                    "Missing required parameter: state. " +
                    "This means the redirect_uri sent to Google (%s) doesn't match what's configured in Google Console. " +
                    "Google redirects to: http://localhost:8100/oauth2/callback/%s " +
                    "Expected: http://localhost:8100/oauth2/callback/google " +
                    "ACTION REQUIRED: In Google Cloud Console, go to APIs & Services > Credentials > Your OAuth 2.0 Client ID. " +
                    "Under 'Authorized redirect URIs', ensure EXACTLY this URI is listed: http://localhost:8100/oauth2/callback/google " +
                    "Remove any other redirect URIs that might be conflicting. The URI must match EXACTLY (no trailing slashes, correct port).",
                    "http://localhost:8100/oauth2/callback/google",
                    providerPath);
            return HttpResponse.badRequest(errorMessage);
        }
        
        try {
            ExternalProfile profile = tokenExchangeService.exchange(code, state);
            SocialLoginResult loginResult = socialLoginService.handle(profile);
            TokenDetails tokenDetails = jwtTokenService.generate(loginResult.user(), loginResult.identity());
            String refreshToken =
                    refreshTokenIssuer.issue(
                            loginResult.user().getId(),
                            loginResult.identity().getProvider(),
                            loginResult.identity().getId());

            OAuthTokenResponse response =
                    new OAuthTokenResponse(
                            tokenDetails.accessToken(),
                            refreshToken,
                            tokenDetails.expiresInSeconds(),
                            "Bearer",
                            loginResult.user().getId(),
                            loginResult.identity().getProvider(),
                            loginResult.identity().getProviderUserId());
            String sessionCode = loginSessionStore.create(response);

            String frontendRedirect = socialOAuthConfigurationProperties.getFrontendRedirectUrl();
            if (StringUtils.isNotEmpty(frontendRedirect)) {
                URI redirectUri = UriBuilder.of(frontendRedirect)
                        .queryParam("code", sessionCode)
                        .build();
                return HttpResponse.redirect(redirectUri);
            }

            return HttpResponse.ok(response);
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest("Invalid state or code: " + e.getMessage());
        } catch (Exception e) {
            return HttpResponse.serverError("OAuth callback processing failed: " + e.getMessage());
        }
    }

    @Get("/finalize")
    public HttpResponse<OAuthTokenResponse> finalizeLogin(@QueryValue("code") String code) {
        return loginSessionStore.consume(code)
                .map(HttpResponse::ok)
                .orElseGet(() -> HttpResponse.badRequest());
    }

    @Get("/config")
    public HttpResponse<OAuthConfigResponse> getConfig() {
        String redirectBaseUrl = socialOAuthConfigurationProperties.getRedirectBaseUrl();
        String frontendRedirectUrl = socialOAuthConfigurationProperties.getFrontendRedirectUrl();
        return HttpResponse.ok(new OAuthConfigResponse(redirectBaseUrl, frontendRedirectUrl));
    }

    @Serdeable
    public record OAuthConfigResponse(String redirectBaseUrl, String frontendRedirectUrl) {}

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

