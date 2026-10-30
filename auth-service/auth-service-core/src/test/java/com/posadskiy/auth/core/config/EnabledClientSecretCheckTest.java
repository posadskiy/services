package com.posadskiy.auth.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnabledClientSecretCheckTest {

    @Test
    void enabledClientRequiresASecret() {
        OAuthClientProperties client = new OAuthClientProperties("system", true, "system", " ", List.of());

        assertThrows(IllegalStateException.class, () -> new EnabledClientSecretCheck(List.of(client)));
    }

    @Test
    void productionRejectsAShortSecret() {
        OAuthClientProperties client = new OAuthClientProperties("system", true, "system", "short-secret", List.of());

        assertThrows(IllegalStateException.class, () -> new EnabledClientSecretCheck(List.of(client), true));
        assertDoesNotThrow(() -> new EnabledClientSecretCheck(List.of(client), false));
    }

    @Test
    void productionAcceptsALongSecret() {
        OAuthClientProperties client =
            new OAuthClientProperties("system", true, "system", "x".repeat(32), List.of());

        assertDoesNotThrow(() -> new EnabledClientSecretCheck(List.of(client), true));
    }

    @Test
    void disabledClientMayHaveNoSecret() {
        OAuthClientProperties client = new OAuthClientProperties("system", false, "system", "", List.of());

        assertDoesNotThrow(() -> new EnabledClientSecretCheck(List.of(client)));
    }
}
