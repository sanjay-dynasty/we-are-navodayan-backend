package com.wearenavodayan.apigateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorrelationIdForwardingIntegrationTests {

    private static HttpServer downstreamServer;

    private static final AtomicReference<String> receivedCorrelationId =
            new AtomicReference<>();

    @Value("${local.server.port}")
    private int gatewayPort;

    @BeforeAll
    static void startDownstreamServer() throws IOException {
        downstreamServer = HttpServer.create(
                new InetSocketAddress("localhost", 0), 0);

        downstreamServer.createContext("/", exchange -> {
            receivedCorrelationId.set(
                    exchange.getRequestHeaders()
                            .getFirst("X-Correlation-ID"));

            byte[] body = "downstream-ok".getBytes();

            exchange.getResponseHeaders().add(
                    "X-Received-Correlation-ID",
                    String.valueOf(receivedCorrelationId.get()));

            exchange.sendResponseHeaders(200, body.length);

            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });

        downstreamServer.start();
    }

    @DynamicPropertySource
    static void configureDownstreamUrl(
            DynamicPropertyRegistry registry) throws IOException {

        if (downstreamServer == null) {
            startDownstreamServer();
        }

        registry.add(
                "services.auth.base-url",
                () -> "http://localhost:"
                        + downstreamServer.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstreamServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    @Test
    void shouldForwardCorrelationIdToDownstreamService() throws Exception {
        String correlationId = "integration-test-123";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + gatewayPort
                                + "/api/v1/auth/test"))
                .header("X-Correlation-ID", correlationId)
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(
                correlationId,
                response.headers()
                        .firstValue("X-Correlation-ID")
                        .orElse(null));

        assertEquals(correlationId, receivedCorrelationId.get());
        assertEquals(
                correlationId,
                response.headers()
                        .firstValue("X-Received-Correlation-ID")
                        .orElse(null));
    }

    @Test
    void shouldGenerateAndForwardCorrelationIdWhenMissing()
            throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + gatewayPort
                                + "/api/v1/auth/test"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        String correlationId = response.headers()
                .firstValue("X-Correlation-ID")
                .orElse(null);

        assertEquals(200, response.statusCode());
        assertNotNull(correlationId);
        assertEquals(correlationId, receivedCorrelationId.get());
    }
}