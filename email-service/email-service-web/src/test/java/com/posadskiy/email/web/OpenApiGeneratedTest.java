package com.posadskiy.email.web;

import io.micronaut.core.io.ResourceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
public class OpenApiGeneratedTest {

    @Test
    void buildGeneratesOpenApi(ResourceLoader resourceLoader) {
        assertTrue(resourceLoader.getResource("META-INF/swagger/email-service-0.1.yml").isPresent());
    }
}
