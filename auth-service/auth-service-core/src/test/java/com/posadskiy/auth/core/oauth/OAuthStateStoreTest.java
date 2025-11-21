package com.posadskiy.auth.core.oauth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OAuthStateStoreTest {

    @Test
    void shouldCreateAndConsumeState() {
        SocialOAuthConfigurationProperties properties = new SocialOAuthConfigurationProperties();
        properties.setStateTtl(Duration.ofMinutes(5));
        OAuthStateStore store = new OAuthStateStore(properties);

        OAuthState state = store.create("google", "http://localhost/callback", "code", "nonce");
        assertNotNull(state);

        assertTrue(store.consume(state.value()).isPresent());
    }
}

