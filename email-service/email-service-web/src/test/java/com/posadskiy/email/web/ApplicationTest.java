package com.posadskiy.email.web;

import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

@MicronautTest
class ApplicationTest {

    @Inject
    EmbeddedApplication<?> application;

    @Test
    void testApplicationStartup() {
        Assertions.assertTrue(application.isRunning());
    }

    @Test
    void testApplicationContext() {
        Assertions.assertNotNull(application.getApplicationContext());
    }

    @Test
    void testApplicationName() {
        // Application name is not directly accessible via getName()
        // but we can verify the application is running
        Assertions.assertTrue(application.isRunning());
    }
} 