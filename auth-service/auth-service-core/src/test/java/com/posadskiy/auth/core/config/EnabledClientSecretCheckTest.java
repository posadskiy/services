package com.posadskiy.auth.core.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnabledClientSecretCheckTest {

    @Test
    void enabledClientRequiresASecret() {
        OAuthClientProperties client = new OAuthClientProperties("system", true, "system", " ", List.of());

        assertThrows(IllegalStateException.class, () -> new EnabledClientSecretCheck(List.of(client)));
    }

    @Test
    void disabledClientMayHaveNoSecret() {
        OAuthClientProperties client = new OAuthClientProperties("system", false, "system", "", List.of());

        assertDoesNotThrow(() -> new EnabledClientSecretCheck(List.of(client)));
    }
}
