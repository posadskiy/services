package com.posadskiy.user.web;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class TracingConfigurationTest {

    @Test
    void testJaegerTracingConfiguration(ApplicationContext applicationContext) {
        // Verify that Jaeger tracing is enabled
        assertTrue(applicationContext.containsProperty("tracing.jaeger.enabled"));
        assertTrue(applicationContext.getProperty("tracing.jaeger.enabled", Boolean.class).orElse(false));
        
        // Verify that the HTTP sender URL is configured
        assertTrue(applicationContext.containsProperty("tracing.jaeger.http-sender.url"));
        String httpSenderUrl = applicationContext.getProperty("tracing.jaeger.http-sender.url", String.class).orElse("");
        assertTrue(httpSenderUrl.contains("jaeger"));
        assertTrue(httpSenderUrl.contains("14268"));
        
        // Verify that sampling is configured
        assertTrue(applicationContext.containsProperty("tracing.jaeger.sampler.probability"));
        Double probability = applicationContext.getProperty("tracing.jaeger.sampler.probability", Double.class).orElse(0.0);
        assertTrue(probability > 0.0);
    }
} 