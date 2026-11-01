package com.posadskiy.user.core.service;

import com.posadskiy.user.core.exception.AuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionalPreferenceTest {

    @Test
    void acceptsLanguageTagsAndIanaZones() {
        assertEquals("en-GB", RegionalPreference.locale("en_GB"));
        assertEquals("Europe/Berlin", RegionalPreference.timeZone("Europe/Berlin"));
        assertNull(RegionalPreference.locale(null));
    }

    @Test
    void rejectsFreeText() {
        assertThrows(AuthException.class, () -> RegionalPreference.locale("not a locale!"));
        assertThrows(AuthException.class, () -> RegionalPreference.timeZone("not a zone"));
        assertThrows(AuthException.class, () -> RegionalPreference.timeZone("CET"));
    }
}
