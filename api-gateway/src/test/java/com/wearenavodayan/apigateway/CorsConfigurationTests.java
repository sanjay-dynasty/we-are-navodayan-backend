package com.wearenavodayan.apigateway;

import com.wearenavodayan.apigateway.config.CorsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigurationTests {

    private static final String ORIGIN = "http://localhost:5173";

    private Map<String, CorsConfiguration> configurations;

    @BeforeEach
    void setUp() {
        TestCorsRegistry registry = new TestCorsRegistry();
        new CorsConfig().addCorsMappings(registry);
        configurations = registry.getRegisteredConfigurations();
    }

    @Test
    void shouldAllowCorsPreflightConfiguration() {
        CorsConfiguration cors = configurations.get("/api/**");

        assertTrue(cors != null);
        assertEquals(ORIGIN, cors.checkOrigin(ORIGIN));
        assertTrue(cors.getAllowedMethods().contains("POST"));
        assertTrue(cors.getAllowedMethods().contains("OPTIONS"));
        assertTrue(cors.getAllowedHeaders().contains("Authorization"));
        assertTrue(cors.getAllowedHeaders().contains("Content-Type"));
        assertTrue(cors.getAllowedHeaders().contains("X-Correlation-ID"));
        assertEquals(Boolean.FALSE, cors.getAllowCredentials());
    }

    @Test
    void shouldExposeCorrelationIdHeader() {
        CorsConfiguration cors = configurations.get("/api/**");

        assertTrue(cors != null);
        assertTrue(cors.getExposedHeaders().contains("X-Correlation-ID"));
        assertEquals(3600L, cors.getMaxAge());
    }

    private static class TestCorsRegistry extends CorsRegistry {

        Map<String, CorsConfiguration> getRegisteredConfigurations() {
            return getCorsConfigurations();
        }
    }
}