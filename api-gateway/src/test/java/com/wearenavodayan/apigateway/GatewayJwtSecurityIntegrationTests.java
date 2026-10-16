package com.wearenavodayan.apigateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayJwtSecurityIntegrationTests {

    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String ISSUER = "we-are-navodayan-auth";
    private static final String AUDIENCE = "we-are-navodayan-api";
    private static final String PROFILE_PATH = "/api/v1/user-profiles/me";

    private static HttpServer downstreamServer;

    @Value("${local.server.port}")
    private int gatewayPort;

    @BeforeAll
    static void startDownstreamServer() throws IOException {
        if (downstreamServer == null) {
            downstreamServer = HttpServer.create(
                    new InetSocketAddress("localhost", 0), 0);

            downstreamServer.createContext("/", exchange -> {
                byte[] body = "downstream-ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);

                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });

            downstreamServer.start();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry)
            throws IOException {

        startDownstreamServer();

        registry.add("JWT_SECRET", () -> TEST_SECRET);
        registry.add("services.user.base-url",
                () -> "http://localhost:"
                        + downstreamServer.getAddress().getPort());
    }

    @AfterAll
    static void stopDownstreamServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
            downstreamServer = null;
        }
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        assertEquals(401, sendRequest(null).statusCode());
    }

    @Test
    void shouldRejectMalformedToken() throws Exception {
        assertEquals(401, sendRequest("not-a-valid-jwt").statusCode());
    }

    @Test
    void shouldAllowValidTokenAndForwardRequest() throws Exception {
        String token = createToken(ISSUER, AUDIENCE);

        HttpResponse<String> response = sendRequest(token);

        assertEquals(200, response.statusCode());
        assertEquals("downstream-ok", response.body());
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() throws Exception {
        String token = createToken("wrong-issuer", AUDIENCE);

        assertEquals(401, sendRequest(token).statusCode());
    }

    @Test
    void shouldRejectTokenWithWrongAudience() throws Exception {
        String token = createToken(ISSUER, "wrong-audience");

        assertEquals(401, sendRequest(token).statusCode());
    }

    private HttpResponse<String> sendRequest(String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + gatewayPort + PROFILE_PATH))
                .GET();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return HttpClient.newHttpClient().send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String createToken(String issuer, String audience)
            throws Exception {

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

        String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
                        .getBytes(StandardCharsets.UTF_8));

        long now = Instant.now().getEpochSecond();

        String claims = "{\"sub\":\"test-user\","
                + "\"iss\":\"" + issuer + "\","
                + "\"aud\":[\"" + audience + "\"],"
                + "\"iat\":" + now + ","
                + "\"exp\":" + (now + 300) + ","
                + "\"jti\":\"gateway-security-test\"}";

        String payload = encoder.encodeToString(
                claims.getBytes(StandardCharsets.UTF_8));

        String signingInput = header + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                Base64.getDecoder().decode(TEST_SECRET),
                "HmacSHA256"));

        String signature = encoder.encodeToString(
                mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII)));

        return signingInput + "." + signature;
    }
}
