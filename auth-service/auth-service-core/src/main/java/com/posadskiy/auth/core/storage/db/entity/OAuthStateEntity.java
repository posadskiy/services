package com.posadskiy.auth.core.storage.db.entity;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import java.time.Instant;

@MappedEntity("oauth_state")
public class OAuthStateEntity {

    @Id
    @MappedProperty("state_value")
    private String stateValue;

    private String provider;

    @MappedProperty("redirect_uri")
    private String redirectUri;

    @MappedProperty("code_verifier")
    private String codeVerifier;

    private String nonce;

    @MappedProperty("browser_binding_hash")
    private String browserBindingHash;

    @MappedProperty("code_challenge")
    private String codeChallenge;

    @MappedProperty("expires_at")
    private Instant expiresAt;

    @DateCreated
    private Instant createdAt;

    public String getStateValue() {
        return stateValue;
    }

    public void setStateValue(String stateValue) {
        this.stateValue = stateValue;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getBrowserBindingHash() {
        return browserBindingHash;
    }

    public void setBrowserBindingHash(String browserBindingHash) {
        this.browserBindingHash = browserBindingHash;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
