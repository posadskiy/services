package com.posadskiy.auth.core.service;

import com.posadskiy.auth.core.exception.AuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionalPreferenceTest {

    @Test
    void acceptsLanguageTagsAndIanaZones() {
        assertEquals("en-GB", RegionalPreference.locale("en_GB"));
        assertEquals("de", RegionalPreference.locale("de"));
        assertEquals("Europe/Berlin", RegionalPreference.timeZone("Europe/Berlin"));
        assertNull(RegionalPreference.locale("  "));
        assertNull(RegionalPreference.timeZone(null));
    }

    @Test
    void rejectsFreeText() {
        assertThrows(AuthException.class, () -> RegionalPreference.locale("not a locale!"));
        assertThrows(AuthException.class, () -> RegionalPreference.timeZone("Central European Time"));
        assertNull(RegionalPreference.localeOrNull("not a locale!"));
        assertNull(RegionalPreference.timeZoneOrNull("CET"));
    }
}
