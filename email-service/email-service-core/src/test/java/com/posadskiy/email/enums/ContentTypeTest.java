package com.posadskiy.email.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class ContentTypeTest {

    @Test
    void testContentTypeValues() {
        ContentType[] values = ContentType.values();
        Assertions.assertEquals(2, values.length);
        Assertions.assertEquals(ContentType.Text, values[0]);
        Assertions.assertEquals(ContentType.HTML, values[1]);
    }

    @Test
    void testContentTypeValueOf() {
        Assertions.assertEquals(ContentType.Text, ContentType.valueOf("Text"));
        Assertions.assertEquals(ContentType.HTML, ContentType.valueOf("HTML"));
    }

    @Test
    void testContentTypeOrdinal() {
        Assertions.assertEquals(0, ContentType.Text.ordinal());
        Assertions.assertEquals(1, ContentType.HTML.ordinal());
    }
} 