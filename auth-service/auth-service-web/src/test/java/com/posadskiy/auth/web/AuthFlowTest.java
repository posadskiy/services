package com.posadskiy.auth.web;

import com.posadskiy.auth.api.dto.*;
import com.posadskiy.auth.core.exception.OAuthException;
import com.posadskiy.auth.core.mail.TransactionalMail;
import com.posadskiy.auth.core.oauth.ExternalProfile;
import com.posadskiy.auth.core.service.SocialLoginService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    @Test
    void registerLoginRefreshAndRevoke() {
        UserResponse created = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/users",
                    new RegisterRequest(
                        "flow@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        assertEquals("flow@example.com", created.email());

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
                    new RegisterRequest(
                        "form@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
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
    void clientCredentialsIssuesAccessTokenWithoutRefresh() {
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("client_credentials", null, "system", "system-secret", null, null)),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
        assertEquals(null, tokens.refreshToken());
        assertEquals(null, tokens.userId());
    }

    @Test
    void accountIdIsAStringAndMatchesTheAccessTokenSubject() {
        String createdJson = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "id@example.com", "correcthorse", "http://localhost:3000")),
                String.class);
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
                    new RegisterRequest(
                        "dup@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/users",
                    new RegisterRequest(
                        "DUP@example.com", "correcthorse", "http://localhost:3000"))));
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
                    new RegisterRequest(
                        "me@example.com", "correcthorse", "http://localhost:3000")),
                UserResponse.class);
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
    void oauthAuthorizeStartsPkceFlow() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
            .exchange(HttpRequest.GET("/oauth2/authorize/google").accept(MediaType.APPLICATION_JSON), String.class);
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
                HttpRequest.GET("/oauth2/authorize/google?return_to=https://grammarbar.com/oauth/callback")
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
                .retrieve(HttpRequest.GET(
                    "/oauth2/authorize/google?return_to=https://evil.example/oauth/callback")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void oauthAuthorizeAcceptsAllowlistedReturnUrl() {
        io.micronaut.http.HttpResponse<String> response = client.toBlocking()
            .exchange(
                HttpRequest.GET("/oauth2/authorize/google?return_to=https://costy.posadskiy.com/oauth/callback")
                    .accept(MediaType.APPLICATION_JSON),
                String.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.body().contains("accounts.google.com"));
    }

    @Test
    void unknownOauthProviderIsRejected() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().retrieve(HttpRequest.GET("/oauth2/authorize/facebook")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void weakPasswordIsRejected() {
        HttpClientResponseException ex = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/users",
                    new RegisterRequest("weak@example.com", "short", "http://localhost:3000"))));
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
    void passwordAccountCanLogInBeforeTheVerificationLinkIsOpened() {
        mailer.clear();
        UserResponse created = client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "verify@example.com", "correcthorse", "https://grammarbar.com")),
                UserResponse.class);
        assertFalse(created.emailVerified());

        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("verify@example.com", "correcthorse")),
                TokenResponse.class);
        UserResponse before = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);
        assertFalse(before.emailVerified());

        assertEquals(
                "Alpha <alpha@example.com>",
                mailer.sent().getLast().from());
        String token = tokenFrom(mailer.sent().getLast(), "VERIFY_URL", "https://grammarbar.com/verify-email?token=");
        client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token)));

        UserResponse after = client.toBlocking()
            .retrieve(HttpRequest.GET("/account/me").bearerAuth(tokens.accessToken()), UserResponse.class);
        assertTrue(after.emailVerified());

        HttpClientResponseException reused = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking().exchange(HttpRequest.POST("/account/verify", new VerifyEmailRequest(token))));
        assertEquals(HttpStatus.BAD_REQUEST, reused.getStatus());
    }

    @Test
    void forgotPasswordResetsThePasswordAndRevokesRefreshTokens() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "reset@example.com",
                        "correcthorse",
                        "https://costy.posadskiy.com")),
                UserResponse.class);
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
                    new RegisterRequest(
                        "evil@example.com", "correcthorse", "https://evil.example"))));
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
                        "victim-claim@example.com",
                        "correcthorse",
                        "http://localhost:3000")),
                UserResponse.class);
        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("victim-claim@example.com", "correcthorse")),
                TokenResponse.class);

        SocialLoginService.SocialLoginResult result = socialLoginService.handle(
            new ExternalProfile("google", "google-victim-claim", "victim-claim@example.com", true, "Victim", null));

        assertEquals(created.id(), String.valueOf(result.user().id()));
        assertTrue(result.user().emailVerified());
        assertEquals(null, result.user().passwordHash());
        HttpClientResponseException password = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/login", new LoginRequest("victim-claim@example.com", "correcthorse"))));
        assertEquals(HttpStatus.UNAUTHORIZED, password.getStatus());
        HttpClientResponseException refresh = assertThrows(
            HttpClientResponseException.class,
            () -> client.toBlocking()
                .retrieve(HttpRequest.POST(
                    "/oauth/token",
                    new RefreshRequest("refresh_token", tokens.refreshToken(), null, null, null, null))));
        assertEquals(HttpStatus.UNAUTHORIZED, refresh.getStatus());
    }

    @Test
    void verifiedSocialLoginKeepsThePasswordOfAConfirmedAccount() {
        mailer.clear();
        client.toBlocking()
            .retrieve(
                HttpRequest.POST(
                    "/signup",
                    new RegisterRequest(
                        "victim-verified@example.com",
                        "correcthorse",
                        "http://localhost:3000")),
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
                        "victim-unverified@example.com",
                        "correcthorse",
                        "http://localhost:3000")),
                UserResponse.class);

        assertThrows(
            OAuthException.class,
            () -> socialLoginService.handle(new ExternalProfile(
                "google", "google-victim-unverified", "victim-unverified@example.com", false, "Victim", null)));

        TokenResponse tokens = client.toBlocking()
            .retrieve(
                HttpRequest.POST("/login", new LoginRequest("victim-unverified@example.com", "correcthorse")),
                TokenResponse.class);
        assertNotNull(tokens.accessToken());
    }
}
