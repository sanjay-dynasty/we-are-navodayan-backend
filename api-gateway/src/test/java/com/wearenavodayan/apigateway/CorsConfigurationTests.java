package com.wearenavodayan.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsConfigurationTests {

    @Value("${local.server.port}")
    private int gatewayPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldAllowPreflightFromReactDevelopmentOrigin() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + gatewayPort
                                + "/api/v1/auth/test"))
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header(
                        "Access-Control-Request-Headers",
                        "authorization,content-type,x-correlation-id")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        assertTrue(
                response.statusCode() == 200
                        || response.statusCode() == 204,
                "Unexpected preflight status: " + response.statusCode());

        assertEquals(
                "http://localhost:5173",
                response.headers()
                        .firstValue("Access-Control-Allow-Origin")
                        .orElse(null));

        String allowedMethods = response.headers()
                .firstValue("Access-Control-Allow-Methods")
                .orElse("");

        assertTrue(
                allowedMethods.contains("POST"),
                "POST should be an allowed CORS method");

        String allowedHeaders = response.headers()
                .firstValue("Access-Control-Allow-Headers")
                .orElse("")
                .toLowerCase();

        assertTrue(allowedHeaders.contains("authorization"));
        assertTrue(allowedHeaders.contains("content-type"));
        assertTrue(allowedHeaders.contains("x-correlation-id"));
    }

    @Test
    void shouldExposeCorrelationIdResponseHeader() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + gatewayPort
                                + "/api/v1/auth/test"))
                .header("Origin", "http://localhost:5173")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

        String exposedHeaders = response.headers()
                .firstValue("Access-Control-Expose-Headers")
                .orElse("");

        assertNotNull(exposedHeaders);
        assertTrue(
                exposedHeaders.toLowerCase().contains("x-correlation-id"),
                "X-Correlation-ID should be exposed to frontend JavaScript");
    }
}