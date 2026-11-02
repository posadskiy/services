package com.posadskiy.auth.web;

import com.posadskiy.auth.api.dto.ProblemDetail;
import com.posadskiy.auth.api.dto.TokenResponse;
import com.posadskiy.auth.core.config.AuthProperties;
import com.posadskiy.auth.core.exception.AuthException;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import com.posadskiy.auth.core.oauth.OAuthBrowserCookie;
import com.posadskiy.auth.core.service.TokenService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import jakarta.inject.Singleton;

import java.time.Duration;

/**
 * Browser sessions keep the refresh token in an httpOnly cookie. A page script cannot drop
 * {@code Sec-Fetch-Mode} or {@code Sec-Fetch-Site}, so those headers are what marks a browser.
 * Native clients omit them and still receive {@code refresh_token} in the JSON body.
 */
@Singleton
public class RefreshSession {

    public static final String COOKIE = "refresh_token";

    private final AuthProperties properties;
    private final TokenService tokenService;

    public RefreshSession(AuthProperties properties, TokenService tokenService) {
        this.properties = properties;
        this.tokenService = tokenService;
    }

    public static boolean isBrowser(HttpRequest<?> request) {
        return request.getHeaders().get("Sec-Fetch-Mode") != null
            || request.getHeaders().get("Sec-Fetch-Site") != null;
    }

    public String refreshGrant(HttpRequest<?> request, @Nullable String presented) {
        if (isBrowser(request)) {
            rejectPresented(presented);
            String token = read(request);
            if (token == null) {
                throw new AuthException("refresh_token is required");
            }
            return token;
        }
        if (presented == null || presented.isBlank()) {
            throw new AuthException("refresh_token is required");
        }
        return presented;
    }

    public MutableHttpResponse<TokenResponse> respond(HttpRequest<?> request, TokenResponse tokens) {
        TokenResponse body = isBrowser(request) ? conceal(tokens) : tokens;
        MutableHttpResponse<TokenResponse> response = HttpResponse.ok(body);
        response.header("Cache-Control", "no-store");
        if (isBrowser(request) && tokens.refreshToken() != null && !tokens.refreshToken().isBlank()) {
            apply(response, issue(tokens.refreshToken()));
        }
        return response;
    }

    public MutableHttpResponse<ProblemDetail> expired(HttpRequest<?> request, InvalidCredentialsException error) {
        MutableHttpResponse<ProblemDetail> response = HttpResponse.status(HttpStatus.UNAUTHORIZED);
        response.body(new ProblemDetail(
            "about:blank", "Unauthorized", 401, error.getMessage(), request.getPath()));
        if (isBrowser(request)) {
            apply(response, clear());
        }
        return response;
    }

    /**
     * Revokes the current family. A browser must not send the token; the cookie is the credential
     * and the response clears it. Logout stays successful when the cookie is already gone.
     */
    public MutableHttpResponse<Void> end(HttpRequest<?> request, @Nullable String presented) {
        if (isBrowser(request)) {
            rejectPresented(presented);
            String token = read(request);
            if (token != null) {
                tokenService.revoke(token);
            }
            MutableHttpResponse<Void> response = HttpResponse.ok();
            apply(response, clear());
            return response;
        }
        if (presented == null || presented.isBlank()) {
            throw new AuthException("token is required");
        }
        tokenService.revoke(presented);
        return HttpResponse.ok();
    }

    private static void rejectPresented(@Nullable String presented) {
        if (presented != null && !presented.isBlank()) {
            throw new AuthException("refresh_token is not accepted from the browser");
        }
    }

    private static TokenResponse conceal(TokenResponse tokens) {
        if (tokens.refreshToken() == null) {
            return tokens;
        }
        return new TokenResponse(
            tokens.accessToken(), null, tokens.expiresIn(), tokens.tokenType(), tokens.userId());
    }

    /** The refresh cookie the browser sent, or null. */
    @Nullable
    public static String cookie(HttpRequest<?> request) {
        return read(request);
    }

    @Nullable
    private static String read(HttpRequest<?> request) {
        return request.getCookies()
            .findCookie(COOKIE)
            .map(Cookie::getValue)
            .filter(value -> !value.isBlank())
            .orElse(null);
    }

    private String issue(String token) {
        return header(token, properties.token().refreshTokenTtl());
    }

    private String clear() {
        return header("", Duration.ZERO);
    }

    /**
     * Written as a raw header because this Micronaut version cannot mark a cookie {@code Partitioned}.
     * That attribute is what lets a cross-site browser call keep the cookie.
     */
    private String header(String value, Duration maxAge) {
        return COOKIE + "=" + value
            + "; Path=" + OAuthBrowserCookie.sessionPath(properties.oauth().redirectBaseUrl())
            + "; Max-Age=" + maxAge.toSeconds()
            + "; Secure; HttpOnly; SameSite=None; Partitioned";
    }

    private static void apply(MutableHttpResponse<?> response, String setCookie) {
        response.header(HttpHeaders.SET_COOKIE, setCookie);
    }
}
