package com.posadskiy.auth.core.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@EachProperty("auth.oauth.providers")
public class OAuthProviderProperties {

    private final String name;
    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String jwksUri;
    private String issuer;
    private String redirectUri;
    private List<String> scopes = new ArrayList<>();
    private boolean pkceRequired = true;
    private String appleTeamId;
    private String appleKeyId;
    private String applePrivateKeyPem;

    public OAuthProviderProperties(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public boolean isPkceRequired() {
        return pkceRequired;
    }

    public void setPkceRequired(boolean pkceRequired) {
        this.pkceRequired = pkceRequired;
    }

    public String getAppleTeamId() {
        return appleTeamId;
    }

    public void setAppleTeamId(String appleTeamId) {
        this.appleTeamId = appleTeamId;
    }

    public String getAppleKeyId() {
        return appleKeyId;
    }

    public void setAppleKeyId(String appleKeyId) {
        this.appleKeyId = appleKeyId;
    }

    public @Nullable String getApplePrivateKeyPem() {
        return applePrivateKeyPem;
    }

    public void setApplePrivateKeyPem(String applePrivateKeyPem) {
        this.applePrivateKeyPem = applePrivateKeyPem;
    }
}
