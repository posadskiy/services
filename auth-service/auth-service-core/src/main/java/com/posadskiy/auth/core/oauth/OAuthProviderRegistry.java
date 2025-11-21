package com.posadskiy.auth.core.oauth;

import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class OAuthProviderRegistry {

    private final Map<String, OAuthProviderConfigurationProperties> providers = new ConcurrentHashMap<>();

    public OAuthProviderRegistry(Collection<OAuthProviderConfigurationProperties> providerConfigurations) {
        providerConfigurations.forEach(
                provider ->
                        providers.put(provider.getName().toLowerCase(), provider));
    }

    public Optional<OAuthProviderConfigurationProperties> find(String provider) {
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(provider.toLowerCase()));
    }

    public OAuthProviderConfigurationProperties require(String provider) {
        return find(provider)
                .filter(OAuthProviderConfigurationProperties::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported OAuth provider: " + provider));
    }
}

