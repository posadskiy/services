package com.posadskiy.auth.core.config;

import io.micronaut.context.annotation.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("auth")
public class AuthProperties {

    private Jwt jwt = new Jwt();
    private Password password = new Password();
    private Token token = new Token();
    private RateLimit rateLimit = new RateLimit();
    private Mail mail = new Mail();
    private OAuth oauth = new OAuth();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Password getPassword() {
        return password;
    }

    public void setPassword(Password password) {
        this.password = password;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public OAuth getOauth() {
        return oauth;
    }

    public void setOauth(OAuth oauth) {
        this.oauth = oauth;
    }

    @ConfigurationProperties("jwt")
    public static class Jwt {
        private String issuer = "https://auth.posadskiy.com";
        private String audience = "api.posadskiy.com";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private String hmacSecret;
        private String privateKeyPem;
        private String publicKeyPem;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public String getHmacSecret() {
            return hmacSecret;
        }

        public void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        public String getPrivateKeyPem() {
            return privateKeyPem;
        }

        public void setPrivateKeyPem(String privateKeyPem) {
            this.privateKeyPem = privateKeyPem;
        }

        public String getPublicKeyPem() {
            return publicKeyPem;
        }

        public void setPublicKeyPem(String publicKeyPem) {
            this.publicKeyPem = publicKeyPem;
        }
    }

    @ConfigurationProperties("password")
    public static class Password {
        private int bcryptCost = 10;

        public int getBcryptCost() {
            return bcryptCost;
        }

        public void setBcryptCost(int bcryptCost) {
            this.bcryptCost = bcryptCost;
        }
    }

    @ConfigurationProperties("token")
    public static class Token {
        private Duration refreshTokenTtl = Duration.ofDays(30);

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }
    }

    @ConfigurationProperties("rate-limit")
    public static class RateLimit {
        private int loginPerMinute = 20;
        private Duration window = Duration.ofMinutes(1);

        public int getLoginPerMinute() {
            return loginPerMinute;
        }

        public void setLoginPerMinute(int loginPerMinute) {
            this.loginPerMinute = loginPerMinute;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

    @ConfigurationProperties("mail")
    public static class Mail {
        private boolean enabled;
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private boolean startTls = true;
        private String from = "noreply@localhost";
        private String publicBaseUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isStartTls() {
            return startTls;
        }

        public void setStartTls(boolean startTls) {
            this.startTls = startTls;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    @ConfigurationProperties("oauth")
    public static class OAuth {
        private Duration stateTtl = Duration.ofMinutes(10);
        private Duration loginCodeTtl = Duration.ofMinutes(5);
        private String redirectBaseUrl = "http://localhost:8100";
        private String frontendRedirectUrl;

        public Duration getStateTtl() {
            return stateTtl;
        }

        public void setStateTtl(Duration stateTtl) {
            this.stateTtl = stateTtl;
        }

        public Duration getLoginCodeTtl() {
            return loginCodeTtl;
        }

        public void setLoginCodeTtl(Duration loginCodeTtl) {
            this.loginCodeTtl = loginCodeTtl;
        }

        public String getRedirectBaseUrl() {
            return redirectBaseUrl;
        }

        public void setRedirectBaseUrl(String redirectBaseUrl) {
            this.redirectBaseUrl = redirectBaseUrl;
        }

        public String getFrontendRedirectUrl() {
            return frontendRedirectUrl;
        }

        public void setFrontendRedirectUrl(String frontendRedirectUrl) {
            this.frontendRedirectUrl = frontendRedirectUrl;
        }
    }
}
