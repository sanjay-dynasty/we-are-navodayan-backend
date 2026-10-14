package com.wearenavodayan.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ApiVersioningConfigurationTests {

    @Autowired
    private Environment environment;

    @Test
    void shouldConfigureCurrentApiVersion() {
        assertEquals("v1", environment.getProperty("api.version.current"));
        assertEquals("/api/v1", environment.getProperty("api.version.base-path"));
    }

    @Test
    void shouldPreserveVersionedApiRoutes() {
        assertEquals(
                "Path=/api/v1/auth/**",
                environment.getProperty(
                        "spring.cloud.gateway.server.webmvc.routes[0].predicates[0]"));

        assertEquals(
                "Path=/api/v1/users/**",
                environment.getProperty(
                        "spring.cloud.gateway.server.webmvc.routes[1].predicates[0]"));

        assertEquals(
                "Path=/api/v1/jnvs/**",
                environment.getProperty(
                        "spring.cloud.gateway.server.webmvc.routes[2].predicates[0]"));
    }
}