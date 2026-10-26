package com.posadskiy.auth.core.oauth;

import com.posadskiy.auth.core.config.OAuthProviderProperties;
import com.posadskiy.auth.core.exception.OAuthException;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class OAuthProviderRegistry {

    private final Map<String, OAuthProviderProperties> providers = new ConcurrentHashMap<>();

    public OAuthProviderRegistry(Collection<OAuthProviderProperties> providerConfigurations) {
        providerConfigurations.forEach(provider -> providers.put(provider.name().toLowerCase(), provider));
    }

    public Optional<OAuthProviderProperties> find(String provider) {
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(provider.toLowerCase()));
    }

    public OAuthProviderProperties require(String provider) {
        return find(provider)
            .filter(OAuthProviderProperties::enabled)
            .orElseThrow(() -> new OAuthException("Unsupported OAuth provider: " + provider));
    }
}
