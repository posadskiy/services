package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.config.OAuthClientProperties;
import com.posadskiy.auth.core.exception.InvalidCredentialsException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {

    @Test
    void blankConfiguredSecretIsRejected() {
        TokenService service = service(new OAuthClientProperties("system", true, "system", "", List.of("ROLE_SERVICE")));

        assertThrows(InvalidCredentialsException.class, () -> service.clientCredentials("system", ""));
        assertThrows(InvalidCredentialsException.class, () -> service.clientCredentials("system", "system-secret"));
    }

    @Test
    void blankPresentedSecretIsRejected() {
        TokenService service =
                service(new OAuthClientProperties("system", true, "system", "system-secret", List.of("ROLE_SERVICE")));

        assertThrows(InvalidCredentialsException.class, () -> service.clientCredentials("system", ""));
        assertThrows(InvalidCredentialsException.class, () -> service.clientCredentials("system", "   "));
    }

    private static TokenService service(OAuthClientProperties client) {
        return new TokenService(null, null, null, null, List.of(client), null, null);
    }
}
