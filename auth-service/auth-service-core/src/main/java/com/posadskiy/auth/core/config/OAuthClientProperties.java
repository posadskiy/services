package com.posadskiy.auth.core.config;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import java.util.List;

@EachProperty("auth.clients")
public class OAuthClientProperties {

    private final String name;
    private String clientId;
    private String clientSecret;
    private List<String> roles = List.of("ROLE_SERVICE");
    private boolean enabled = true;

    public OAuthClientProperties(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getClientId() {
        return clientId != null ? clientId : name;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
