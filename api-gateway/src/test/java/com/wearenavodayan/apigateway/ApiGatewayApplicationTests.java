package com.wearenavodayan.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private GatewayMvcProperties gatewayMvcProperties;

    @Test
    void contextLoads() {
        assertNotNull(gatewayMvcProperties);
    }

    @Test
    void shouldConfigureAuthUserJnvAndUserProfileRoutes() {
        Map<String, RouteProperties> routes = gatewayMvcProperties.getRoutes()
                .stream()
                .collect(Collectors.toMap(RouteProperties::getId, route -> route));

        assertEquals(4, routes.size());

        assertRoute(routes, "auth-service-route",
                "http://localhost:8081", "/api/v1/auth/**");

        assertRoute(routes, "user-service-route",
                "http://localhost:8082", "/api/v1/users/**");

        assertRoute(routes, "jnv-service-route",
                "http://localhost:8083", "/api/v1/jnvs/**");

        assertRoute(routes, "user-profile-service-route",
                "http://localhost:8082", "/api/v1/user-profiles/**");
    }

    private void assertRoute(
            Map<String, RouteProperties> routes,
            String routeId,
            String expectedUri,
            String expectedPath) {

        RouteProperties route = routes.get(routeId);

        assertNotNull(route, "Missing route: " + routeId);
        assertEquals(expectedUri, route.getUri().toString());

        assertTrue(
                route.getPredicates().stream().anyMatch(predicate ->
                        "Path".equals(predicate.getName())
                                && predicate.getArgs().containsValue(expectedPath)),
                "Missing Path predicate " + expectedPath + " for " + routeId
        );
    }
}
