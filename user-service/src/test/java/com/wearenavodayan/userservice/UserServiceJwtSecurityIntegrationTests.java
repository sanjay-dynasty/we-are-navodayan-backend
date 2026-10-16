package com.wearenavodayan.userservice;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class UserServiceJwtSecurityIntegrationTests {

    private static final String ISSUER = "we-are-navodayan-auth";
    private static final String AUDIENCE = "we-are-navodayan-api";
    private static final String TEST_SECRET_BASE64 =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final byte[] TEST_SECRET =
            Base64.getDecoder().decode(TEST_SECRET_BASE64);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @DynamicPropertySource
    static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret", () -> TEST_SECRET_BASE64);
        registry.add("services.internal.api-key", () -> "test-internal-key");
    }

    @Test
    void shouldRejectInternalProfileRequestWithoutServiceKey() throws Exception {
        HttpResponse<String> response = postInternalProfile(null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldRejectInternalProfileRequestWithInvalidServiceKey() throws Exception {
        HttpResponse<String> response = postInternalProfile("incorrect-test-key");

        assertThat(response.statusCode()).isEqualTo(401);
    }
    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {
        HttpResponse<String> response = get("/api/v1/security-test", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldRejectMalformedJwt() throws Exception {
        HttpResponse<String> response =
                get("/api/v1/security-test", "not-a-valid-jwt");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldRejectJwtSignedWithWrongSecret() throws Exception {
        String token = createToken(
                ISSUER,
                AUDIENCE,
                Base64.getDecoder().decode(
                        "YW5vdGhlci10ZXN0LXNlY3JldC0xMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMw=="));

        HttpResponse<String> response =
                get("/api/v1/security-test", token);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldRejectJwtWithWrongIssuer() throws Exception {
        String token = createToken("wrong-issuer", AUDIENCE, TEST_SECRET);

        HttpResponse<String> response =
                get("/api/v1/security-test", token);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldRejectJwtWithWrongAudience() throws Exception {
        String token = createToken(ISSUER, "wrong-audience", TEST_SECRET);

        HttpResponse<String> response =
                get("/api/v1/security-test", token);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void shouldAllowRequestWithValidJwtThroughSecurityFilter() throws Exception {
        String token = createToken(ISSUER, AUDIENCE, TEST_SECRET);

        HttpResponse<String> response =
                get("/api/v1/security-test", token);

        // No controller exists at this test path. A 404 confirms that
        // the request passed authentication and reached request handling.
        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void shouldAllowPublicHealthEndpoint() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();

        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return HTTP_CLIENT.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String createToken(
            String issuer,
            String audience,
            byte[] signingSecret) throws Exception {

        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims);

        jwt.sign(new MACSigner(signingSecret));

        return jwt.serialize();
    }

    private HttpResponse<String> postInternalProfile(String serviceKey)
            throws Exception {
        String body = """
                {
                  "authUserId": "%s",
                  "fullName": "Test Navodayan"
                }
                """.formatted(UUID.randomUUID());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port
                                + "/internal/api/v1/user-profiles"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (serviceKey != null) {
            builder.header("X-Internal-Service-Key", serviceKey);
        }

        return HTTP_CLIENT.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
