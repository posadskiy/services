package com.posadskiy.auth.web;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.service.SocialLoginService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
class AuthFlowTest extends PostgresMicronautTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    CapturingTransactionalMailer mailer;

    @Inject
    SocialLoginService socialLoginService;

    @Inject
    javax.sql.DataSource dataSource;

    private static String jwtSubject(String accessToken) {
        String payload = accessToken.split("\\.")[1];
        String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        Matcher subject = Pattern.compile("\"sub\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertTrue(subject.find());
        return subject.group(1);
    }

    private static String tokenFrom(TransactionalMail mail, String variable, String prefix) {
        String url = mail.variables().get(variable);
        assertTrue(url.startsWith(prefix));
        return url.substring(prefix.length());
    }

    private void confirmLatest(String origin) {
        String token = tokenFrom(mailer.sent().getLast(), "VERIFY_URL", origin + "/verify-email?token=");
        client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token)));
    }

    @Test
    void registerLoginRefreshAndRevoke() {
        UserResponse created = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/users",
                    new RegisterRequest("flow@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        assertEquals("flow@example.com", created.email());
        confirmLatest("http://localhost:3000");

        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("flow@example.com", "correcthorse")),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
        assertNotNull(tokens.refreshToken());
        assertTrue(tokens.expiresIn() <= 900);

        HttpClientResponseException denied = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST("/login", new LoginRequest("flow@example.com", "wrong-password"))));
        assertEquals(HttpStatus.UNAUTHORIZED, denied.getStatus());

        TokenResponse refreshed = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("refresh_token", tokens.refreshToken(), null, null, null, null)),
                TokenResponse.class);
        assertNotNull(refreshed.accessToken());
        assertNotEquals(tokens.refreshToken(), refreshed.refreshToken());

        HttpClientResponseException reuse = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("refresh_token", tokens.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, reuse.getStatus());

        HttpClientResponseException successor = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest(
                        "refresh_token", refreshed.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, successor.getStatus());

        client.toBlocking().exchange(HttpRequest.POST("/oauth/revoke", new RevokeRequest(refreshed.refreshToken())));

        HttpClientResponseException afterRevoke = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest(
                        "refresh_token", refreshed.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, afterRevoke.getStatus());
    }

    @Test
    void formRefreshRotatesTheToken() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/users",
                    new RegisterRequest("form@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("form@example.com", "correcthorse")),
                TokenResponse.class);

        TokenResponse refreshed = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                        "/oauth/token",
                        "grant_type=refresh_token&refresh_token=" + tokens.refreshToken())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED),
                TokenResponse.class);
        assertNotNull(refreshed.accessToken());
        assertNotEquals(tokens.refreshToken(), refreshed.refreshToken());
    }

    @Test
    void browserKeepsTheRefreshTokenInAnHttpOnlyCookie() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/users",
                    new RegisterRequest("browser@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");

        HttpResponse<TokenResponse> login = client.toBlocking()
            .exchange(
                HttpRequest.POST("/login", new LoginRequest("browser@example.com", "correcthorse"))
                    .header("Sec-Fetch-Mode", "cors"),
                TokenResponse.class);
        assertNotNull(login.body().accessToken());
        assertNull(login.body().refreshToken());
        String issued = setCookie(login);
        assertTrue(issued.toLowerCase().contains("httponly"));
        assertTrue(issued.toLowerCase().contains("secure"));
        assertTrue(issued.toLowerCase().contains("samesite=none"));
        assertTrue(issued.toLowerCase().contains("partitioned"));
        assertTrue(issued.contains("Path=/"));
        String current = cookieValue(issued);

        HttpClientResponseException presented = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .exchange(
                    HttpRequest.POST(
                            "/oauth/token",
                            new RefreshRequest("refresh_token", current, null, null, null, null))
                        .header("Sec-Fetch-Mode", "cors")
                        .cookie(Cookie.of(RefreshSession.COOKIE, current)),
                    TokenResponse.class));
        assertEquals(HttpStatus.BAD_REQUEST, presented.getStatus());

        HttpResponse<TokenResponse> refreshed = client.toBlocking()
            .exchange(
                browserRefresh(current),
                TokenResponse.class);
        assertNull(refreshed.body().refreshToken());
        assertNotNull(refreshed.body().accessToken());
        String rotated = cookieValue(setCookie(refreshed));
        assertNotEquals(current, rotated);

        HttpResponse<?> loggedOut = client.toBlocking()
            .exchange(
                HttpRequest.POST("/logout", Map.of())
                    .header("Sec-Fetch-Mode", "cors")
                    .cookie(Cookie.of(RefreshSession.COOKIE, rotated)));
        String cleared = setCookie(loggedOut).toLowerCase();
        assertTrue(cleared.contains("max-age=0"));

        HttpClientResponseException afterLogout = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(browserRefresh(rotated), TokenResponse.class));
        assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatus());
    }

    @Test
    void browserFormRefreshTokenIsRejected() {
        HttpClientResponseException error = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .exchange(
                    HttpRequest.POST("/oauth/token", "grant_type=refresh_token&refresh_token=from-the-browser")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("Sec-Fetch-Site", "cross-site")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertTrue(error.getResponse().getBody(String.class).orElse("").contains("not accepted from the browser"));
    }

    private static HttpRequest<?> browserRefresh(String refreshToken) {
        return HttpRequest.POST(
                "/oauth/token", new RefreshRequest("refresh_token", null, null, null, null, null))
            .header("Sec-Fetch-Mode", "cors")
            .cookie(Cookie.of(RefreshSession.COOKIE, refreshToken));
    }

    private static String setCookie(HttpResponse<?> response) {
        return response.getHeaders().getAll("Set-Cookie").stream()
            .filter(header -> header.startsWith(RefreshSession.COOKIE + "="))
            .findFirst()
            .orElseThrow();
    }

    private static String cookieValue(String setCookie) {
        String pair = setCookie.split(";", 2)[0];
        return pair.substring((RefreshSession.COOKIE + "=").length());
    }

    @Test
    void clientCredentialsIssuesAccessTokenWithoutRefresh() {
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("client_credentials", null, "system", "system-secret", null, null)),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
        assertNull(tokens.refreshToken());
        assertNull(tokens.userId());
    }

    @Test
    void costyProfileClientCredentialsIssuesAccessTokenWithoutRefresh() {
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest(
                        "client_credentials", null, "costy-profile", "costy-profile-secret", null, null)),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
        assertNull(tokens.refreshToken());
        assertNull(tokens.userId());
    }

    @Test
    void clientCredentialsAreRefusedWhenTheRequestCameThroughIngress() {
        HttpClientResponseException error = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(
                    HttpRequest.POST(
                            "/oauth/token",
                            new RefreshRequest("client_credentials", null, "system", "system-secret", null, null))
                        .header("X-Forwarded-For", "203.0.113.9"),
                    TokenResponse.class));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void accountIdIsAStringAndMatchesTheAccessTokenSubject() {
        String createdJson = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("id@example.com", "correcthorse", "http://localhost:3000")),
                String.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("id@example.com", "correcthorse")),
                TokenResponse.class);
        String loginJson = client.toBlocking()
            .retrieve(HttpRequest.POST("/login", new LoginRequest("id@example.com", "correcthorse")), String.class);
        UserResponse me = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);

        assertTrue(me.id().matches("\\d+"));
        assertEquals(me.id(), tokens.userId());
        assertEquals(me.id(), jwtSubject(tokens.accessToken()));
        assertTrue(createdJson.contains("\"id\":\"" + me.id() + "\""));
        assertFalse(createdJson.contains("\"id\":" + me.id()));
        assertTrue(loginJson.contains("\"user_id\":\"" + me.id() + "\""));
        assertFalse(loginJson.contains("\"user_id\":" + me.id()));
    }

    @Test
    void duplicateEmailIsConflict() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/users",
                    new RegisterRequest("dup@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/users",
                    new RegisterRequest("DUP@example.com", "correcthorse", "http://localhost:3000"))));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void systemPasswordLoginIsRejected() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST("/login", new LoginRequest("system", "anypassword"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void accountMeRequiresBearerAndReturnsProfile() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/users",
                    new RegisterRequest("me@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("me@example.com", "correcthorse")),
                TokenResponse.class);

        HttpClientResponseException anonymous = assertThrows(
            HttpClientResponseException.class, () -> client.toBlocking().retrieve(HttpRequest.GET("/account/me")));
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getStatus());

        UserResponse me = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);
        assertEquals("me@example.com", me.email());
    }

    @Test
    void discoveryAndKeysArePublic() {
        String discovery =
            client.toBlocking().retrieve(HttpRequest.GET("/.well-known/openid-configuration"), String.class);
        assertTrue(discovery.contains("https://auth.test"));
        assertTrue(discovery.contains("refresh_token"));

        io.micronaut.http.HttpResponse<String> keys =
            client.toBlocking().exchange(HttpRequest.GET("/keys"), String.class);
        assertEquals(HttpStatus.OK, keys.getStatus());
        assertNotNull(keys.body());
        assertTrue(keys.getHeaders().contains("X-Content-Type-Options"));
    }

    @Test
    void oauthAuthorizeRequiresACodeChallenge() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().retrieve(HttpRequest.GET("/oauth2/authorize/google")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void oauthAuthorizeStartsPkceFlow() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
            .exchange(
                HttpRequest.GET("/oauth2/authorize/google?code_challenge=test-challenge")
                    .accept(MediaType.APPLICATION_JSON),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        String body = response.body();
        assertTrue(body.contains("accounts.google.com"));
        assertTrue(body.contains("code_challenge"));
        assertTrue(body.contains("test-google-client"));
        assertTrue(response.getHeaders().getAll("Set-Cookie").stream()
            .anyMatch(value -> value.startsWith("oauth_browser=") && value.contains("Path=/oauth2")));
    }

    @Test
    void oauthAuthorizeRedirectsABrowserAndScopesTheBindingCookie() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
            .exchange(
                HttpRequest.GET(
                        "/oauth2/authorize/google?code_challenge=test-challenge&return_to=https://grammarbar.com/oauth/callback")
                    .accept(MediaType.TEXT_HTML),
                String.class);
        assertTrue(response.getStatus().getCode() >= 300 && response.getStatus().getCode() < 400);
        assertTrue(response.getHeaders().get("Location").contains("accounts.google.com"));
        assertTrue(response.getHeaders().get("Location").contains("redirect_uri="));
        assertTrue(response.getHeaders().getAll("Set-Cookie").stream()
            .anyMatch(value -> value.startsWith("oauth_browser=") && value.contains("Path=/oauth2")));
    }

    @Test
    void oauthAuthorizeRejectsReturnUrlOutsideAllowlist() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(
                    HttpRequest.GET(
                        "/oauth2/authorize/google?code_challenge=test-challenge&return_to=https://evil.example/oauth/callback")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void oauthAuthorizeAcceptsAllowlistedReturnUrl() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
            .exchange(
                HttpRequest.GET(
                        "/oauth2/authorize/google?code_challenge=test-challenge&return_to=https://costy.posadskiy.com/oauth/callback")
                    .accept(MediaType.APPLICATION_JSON),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.body().contains("accounts.google.com"));
    }

    @Test
    void oauthAuthorizeStartsGitHub() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
            .exchange(
                HttpRequest.GET("/oauth2/authorize/github?code_challenge=test-challenge")
                    .accept(MediaType.APPLICATION_JSON),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        String body = response.body();
        assertTrue(body.contains("github.com/login/oauth/authorize"));
        assertTrue(body.contains("test-github-client"));
        assertTrue(body.contains("read%3Auser") || body.contains("read:user"));
        assertTrue(body.contains("user%3Aemail") || body.contains("user:email"));
    }

    @Test
    void unknownOauthProviderIsRejected() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.GET("/oauth2/authorize/facebook?code_challenge=test-challenge")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void weakPasswordIsRejected() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/users", new RegisterRequest("weak@example.com", "short", "http://localhost:3000"))));
        assertTrue(ex.getStatus().getCode() >= 400);
    }

    @Test
    void clientCredentialsRejectsUnknownClient() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("client_credentials", null, "nope", "wrong", null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void passwordAccountCannotLogInBeforeTheVerificationLinkIsOpened() {
        mailer.clear();
        UserResponse created = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("verify@example.com", "correcthorse", "https://grammarbar.com")),
                UserResponse.class);
        assertFalse(created.emailVerified());

        assertEquals(1, mailer.sent().size());
        HttpClientResponseException blocked = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST("/login", new LoginRequest("verify@example.com", "correcthorse"))));
        assertEquals(HttpStatus.FORBIDDEN, blocked.getStatus());
        assertEquals(1, mailer.sent().size(), "unverified login must not resend the verification email");

        assertEquals("Alpha <alpha@example.com>", mailer.sent().getLast().from());
        String token = tokenFrom(mailer.sent().getLast(), "VERIFY_URL", "https://grammarbar.com/verify-email?token=");
        client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token)));

        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("verify@example.com", "correcthorse")),
                TokenResponse.class);
        UserResponse after = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);
        assertTrue(after.emailVerified());

        HttpClientResponseException reused = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token))));
        assertEquals(HttpStatus.BAD_REQUEST, reused.getStatus());
    }

    @Test
    void anonymousResendVerificationSendsAnotherLinkWithoutLeakingUnknownEmails() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("resend@example.com", "correcthorse", "https://grammarbar.com")),
                UserResponse.class);
        assertEquals(1, mailer.sent().size());

        client.toBlocking()
            .exchange(HttpRequest.POST(
                "/account/verification-email",
                new ResendVerificationRequest("resend@example.com", "https://grammarbar.com")));
        assertEquals(2, mailer.sent().size());
        String token = tokenFrom(mailer.sent().getLast(), "VERIFY_URL", "https://grammarbar.com/verify-email?token=");
        client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token)));

        mailer.clear();
        client.toBlocking()
            .exchange(HttpRequest.POST(
                "/account/verification-email",
                new ResendVerificationRequest("nobody@example.com", "https://grammarbar.com")));
        assertTrue(mailer.sent().isEmpty());
    }

    @Test
    void forgotPasswordResetsThePasswordAndRevokesRefreshTokens() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "reset@example.com", "correcthorse", "https://costy.posadskiy.com")),
                UserResponse.class);
        confirmLatest("https://costy.posadskiy.com");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("reset@example.com", "correcthorse")),
                TokenResponse.class);
        mailer.clear();

        client.toBlocking()
            .exchange(HttpRequest.POST(
                "/account/forgot-password",
                new ForgotPasswordRequest("reset@example.com", "https://costy.posadskiy.com")));
        assertEquals("Beta <beta@example.com>", mailer.sent().getLast().from());
        String token =
            tokenFrom(mailer.sent().getLast(), "RESET_URL", "https://costy.posadskiy.com/reset-password?token=");
        client.toBlocking()
            .exchange(
                HttpRequest.POST("/account/reset-password", new ResetPasswordRequest(token, "correcthorse2")));

        HttpClientResponseException oldRefresh = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("refresh_token", tokens.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, oldRefresh.getStatus());

        TokenResponse next = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("reset@example.com", "correcthorse2")),
                TokenResponse.class);
        assertNotNull(next.accessToken());
        HttpClientResponseException oldPassword = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST("/login", new LoginRequest("reset@example.com", "correcthorse"))));
        assertEquals(HttpStatus.UNAUTHORIZED, oldPassword.getStatus());
        UserResponse me = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(next.accessToken()), UserResponse.class);
        assertTrue(me.emailVerified());
    }

    @Test
    void passwordResetConfirmsAnUnverifiedAccount() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "reset-unverified@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        client.toBlocking()
            .exchange(HttpRequest.POST(
                "/account/forgot-password",
                new ForgotPasswordRequest("reset-unverified@example.com", "http://localhost:3000")));
        String token = tokenFrom(mailer.sent().getLast(), "RESET_URL", "http://localhost:3000/reset-password?token=");
        client.toBlocking()
            .exchange(
                HttpRequest.POST("/account/reset-password", new ResetPasswordRequest(token, "correcthorse2")));

        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("reset-unverified@example.com", "correcthorse2")),
                TokenResponse.class);
        UserResponse me = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);
        assertTrue(me.emailVerified());

        socialLoginService.handle(new ExternalProfile(
            "google", "google-reset-unverified", "reset-unverified@example.com", true, "Reset", null));
        TokenResponse still = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("reset-unverified@example.com", "correcthorse2")),
                TokenResponse.class);
        assertNotNull(still.accessToken());
    }

    @Test
    void forgotPasswordForAnUnknownAddressDoesNotSendMail() {
        mailer.clear();
        io.micronaut.http.HttpResponse<?> response = client.toBlocking()
            .exchange(HttpRequest.POST(
                "/account/forgot-password",
                new ForgotPasswordRequest("missing@example.com", "https://grammarbar.com")));
        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
        assertTrue(mailer.sent().isEmpty());
    }

    @Test
    void disallowedLinkOriginIsRejectedBeforeTheAccountIsCreated() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("evil@example.com", "correcthorse", "https://evil.example"))));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        HttpClientResponseException login = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST("/login", new LoginRequest("evil@example.com", "correcthorse"))));
        assertEquals(HttpStatus.UNAUTHORIZED, login.getStatus());
    }

    @Test
    void verifiedSocialLoginTakesAnUnverifiedPasswordAccount() {
        UserResponse created = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "victim-claim@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        HttpClientResponseException blocked = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/login", new LoginRequest("victim-claim@example.com", "correcthorse"))));
        assertEquals(HttpStatus.FORBIDDEN, blocked.getStatus());

        SocialLoginService.SocialLoginResult result = socialLoginService.handle(
            new ExternalProfile("google", "google-victim-claim", "victim-claim@example.com", true, "Victim", null));

        assertEquals(created.id(), String.valueOf(result.user().id()));
        assertTrue(result.user().emailVerified());
        assertNull(result.user().passwordHash());
        HttpClientResponseException password = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/login", new LoginRequest("victim-claim@example.com", "correcthorse"))));
        assertEquals(HttpStatus.UNAUTHORIZED, password.getStatus());
    }

    @Test
    void verifiedSocialLoginKeepsThePasswordOfAConfirmedAccount() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "victim-verified@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        String token = tokenFrom(mailer.sent().getLast(), "VERIFY_URL", "http://localhost:3000/verify-email?token=");
        client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token)));

        socialLoginService.handle(new ExternalProfile(
            "google", "google-victim-verified", "victim-verified@example.com", true, "Victim", null));

        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("victim-verified@example.com", "correcthorse")),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
    }

    @Test
    void unverifiedSocialLoginDoesNotAttachToAnExistingAccount() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "victim-unverified@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);

        assertThrows(
            OAuthException.class,
            () -> socialLoginService.handle(new ExternalProfile(
                "google", "google-victim-unverified", "victim-unverified@example.com", false, "Victim", null)));

        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("victim-unverified@example.com", "correcthorse")),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
    }

    @Test
    void sessionsPasswordChangeExportAndErase() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("security@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/login",
                    new LoginRequest("security@example.com", "correcthorse", "http://localhost:3000")),
                TokenResponse.class);

        List<SessionResponse> sessions = List.of(client.toBlocking()
            .retrieve(
                HttpRequest.GET("/account/sessions").bearerAuth(tokens.accessToken()),
                SessionResponse[].class));
        assertFalse(sessions.isEmpty());

        client.toBlocking()
            .exchange(HttpRequest.POST(
                    "/account/password",
                    new ChangePasswordRequest("correcthorse", "correcthorse2"))
                .bearerAuth(tokens.accessToken()));

        Map<?, ?> exported = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/export").bearerAuth(tokens.accessToken()), Map.class);
        assertEquals("security@example.com", exported.get("email"));

        client.toBlocking()
            .exchange(HttpRequest.POST("/account/erase", new EraseAccountRequest("correcthorse2", "DELETE"))
                .bearerAuth(tokens.accessToken()));

        HttpClientResponseException gone = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/login", new LoginRequest("security@example.com", "correcthorse2", "http://localhost:3000"))));
        assertEquals(HttpStatus.UNAUTHORIZED, gone.getStatus());
    }

    @Test
    void changeEmailLinkOpensTheConfirmScreen() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("change-email@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("change-email@example.com", "correcthorse")),
                TokenResponse.class);

        client.toBlocking()
            .exchange(HttpRequest.POST(
                    "/account/email",
                    new ChangeEmailRequest(
                        "change-email-next@example.com", "http://localhost:3000", "correcthorse"))
                .bearerAuth(tokens.accessToken()));

        String token = tokenFrom(
            mailer.sent().getLast(), "VERIFY_URL", "http://localhost:3000/confirm-email?token=");
        client.toBlocking()
            .exchange(HttpRequest.POST("/account/email/confirm", new ConfirmEmailRequest(token)));

        TokenResponse next = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("change-email-next@example.com", "correcthorse")),
                TokenResponse.class);
        assertNotNull(next.accessToken());
    }

    @Test
    void passkeyOptionsExposeTheCeremonyChallenge() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("passkey-options@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("passkey-options@example.com", "correcthorse")),
                TokenResponse.class);

        Map<?, ?> options = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/account/passkeys/options", Map.of())
                    .bearerAuth(tokens.accessToken())
                    .header("Origin", "http://localhost:3000"),
                Map.class);
        assertNotNull(options.get("challengeId"));
        Map<?, ?> publicKey = (Map<?, ?>) options.get("publicKey");
        assertNotNull(publicKey.get("challenge"));
        assertFalse(publicKey.containsKey("publicKey"));
    }

    @Test
    void revokeSessionRemovesThatFamily() {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest("revoke-session@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("revoke-session@example.com", "correcthorse")),
                TokenResponse.class);

        List<SessionResponse> sessions = List.of(client.toBlocking()
            .retrieve(
                HttpRequest.GET("/account/sessions").bearerAuth(tokens.accessToken()),
                SessionResponse[].class));
        assertEquals(1, sessions.size());

        client.toBlocking()
            .exchange(HttpRequest.DELETE("/account/sessions/" + sessions.getFirst().id())
                .bearerAuth(tokens.accessToken()));

        List<SessionResponse> after = List.of(client.toBlocking()
            .retrieve(
                HttpRequest.GET("/account/sessions").bearerAuth(tokens.accessToken()),
                SessionResponse[].class));
        assertTrue(after.isEmpty());

        HttpClientResponseException missing = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .exchange(HttpRequest.DELETE("/account/sessions/not-a-uuid")
                    .bearerAuth(tokens.accessToken())));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatus());
    }

    private static final String APP_VERIFIER = "native-app-verifier-0123456789-abcdefghijklmnop";

    private static String appChallenge() throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(APP_VERIFIER.getBytes(StandardCharsets.US_ASCII)));
    }

    /** Signs in the way the site does and returns the refresh cookie value. */
    private String browserSignIn(String email) {
        client.toBlocking()
            .retrieve(
                HttpRequest.POST("/users", new RegisterRequest(email, "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        confirmLatest("http://localhost:3000");
        HttpResponse<TokenResponse> login = client.toBlocking()
            .exchange(
                HttpRequest.POST("/login", new LoginRequest(email, "correcthorse"))
                    .header("Sec-Fetch-Mode", "cors"),
                TokenResponse.class);
        return cookieValue(setCookie(login));
    }

    private static HttpRequest<?> appCodeRequest(String cookie, String origin, String challenge) {
        var request = HttpRequest.POST("/account/app-code", Map.of("code_challenge", challenge))
            .header("Sec-Fetch-Mode", "cors")
            .header("Origin", origin);
        return cookie == null ? request : request.cookie(Cookie.of(RefreshSession.COOKIE, cookie));
    }

    private HttpStatus appCodeStatus(HttpRequest<?> request) {
        return assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().retrieve(request, AppCodeResponse.class)).getStatus();
    }

    @Test
    void recentBrowserSignInHandsANativeAppItsOwnSession() throws Exception {
        String cookie = browserSignIn("app-code@example.com");
        String challenge = appChallenge();

        AppCodeResponse issued = client.toBlocking()
            .retrieve(appCodeRequest(cookie, "http://localhost:3000", challenge), AppCodeResponse.class);

        HttpResponse<TokenResponse> redeemed = client.toBlocking()
            .exchange(
                HttpRequest.POST("/oauth2/finalize", new OAuthFinalizeRequest(issued.code(), APP_VERIFIER))
                    .header("User-Agent", "Grammarbar for Mac"),
                TokenResponse.class);
        TokenResponse app = redeemed.body();
        assertNotNull(app.refreshToken());
        assertNotEquals(cookie, app.refreshToken());

        List<SessionResponse> sessions = List.of(client.toBlocking()
            .retrieve(
                HttpRequest.GET("/account/sessions").bearerAuth(app.accessToken()),
                SessionResponse[].class));
        assertEquals(2, sessions.size());
        assertTrue(sessions.stream().anyMatch(session -> "Grammarbar for Mac".equals(session.userAgent())));

        assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().retrieve(
                HttpRequest.POST("/oauth2/finalize", new OAuthFinalizeRequest(issued.code(), APP_VERIFIER)),
                TokenResponse.class));

        AppCodeResponse other = client.toBlocking()
            .retrieve(appCodeRequest(cookie, "http://localhost:3000", challenge), AppCodeResponse.class);
        assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().retrieve(
                HttpRequest.POST("/oauth2/finalize", new OAuthFinalizeRequest(other.code(), "wrong-verifier")),
                TokenResponse.class));
    }

    @Test
    void appCodeNeedsTheBrowserCookieFromAnAllowedSite() throws Exception {
        String cookie = browserSignIn("app-code-guard@example.com");
        String challenge = appChallenge();

        TokenResponse bearerOnly = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("app-code-guard@example.com", "correcthorse")),
                TokenResponse.class);
        assertEquals(
            HttpStatus.BAD_REQUEST,
            appCodeStatus(HttpRequest.POST("/account/app-code", Map.of("code_challenge", challenge))
                .bearerAuth(bearerOnly.accessToken())));

        assertEquals(HttpStatus.UNAUTHORIZED, appCodeStatus(appCodeRequest(null, "http://localhost:3000", challenge)));
        assertEquals(
            HttpStatus.UNAUTHORIZED,
            appCodeStatus(appCodeRequest("not-a-session", "http://localhost:3000", challenge)));
        assertEquals(HttpStatus.BAD_REQUEST, appCodeStatus(appCodeRequest(cookie, "https://evil.example", challenge)));
        assertEquals(HttpStatus.BAD_REQUEST, appCodeStatus(appCodeRequest(cookie, "http://localhost:3000", "short")));

        String rotated = cookieValue(setCookie(client.toBlocking().exchange(browserRefresh(cookie), TokenResponse.class)));
        assertEquals(HttpStatus.UNAUTHORIZED, appCodeStatus(appCodeRequest(cookie, "http://localhost:3000", challenge)));
        assertNotNull(client.toBlocking()
            .retrieve(appCodeRequest(rotated, "http://localhost:3000", challenge), AppCodeResponse.class)
            .code());
    }

    @Test
    void anOldBrowserSignInMustSignInAgainBeforeHandingOff() throws Exception {
        String cookie = browserSignIn("app-code-stale@example.com");
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "UPDATE refresh_token SET created_at = created_at - INTERVAL '1 hour' "
                     + "WHERE user_id = (SELECT id FROM users WHERE email = ?)")) {
            statement.setString(1, "app-code-stale@example.com");
            assertEquals(1, statement.executeUpdate());
        }
        assertEquals(HttpStatus.FORBIDDEN, appCodeStatus(appCodeRequest(cookie, "http://localhost:3000", appChallenge())));
    }
}
