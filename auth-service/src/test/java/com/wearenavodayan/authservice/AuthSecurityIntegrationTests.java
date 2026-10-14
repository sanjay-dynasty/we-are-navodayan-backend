package com.wearenavodayan.authservice;

import java.time.Instant;
import java.util.List;

import com.wearenavodayan.authservice.entity.UserAccount;
import com.wearenavodayan.authservice.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String validToken;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();

        UserAccount account = new UserAccount();
        account.setEmail("security.test@example.com");
        account.setFullName("Security Test");
        account.setPasswordHash(passwordEncoder.encode("SecurePassword123!"));
        UserAccount saved = userAccountRepository.saveAndFlush(account);

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("we-are-navodayan-auth")
                .subject(saved.getId().toString())
                .audience(List.of("we-are-navodayan-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .id("security-test-token")
                .claim("role", "USER")
                .build();

        validToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsValidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedEndpointRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsExpiredToken() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet expiredClaims = JwtClaimsSet.builder()
                .issuer("we-are-navodayan-auth")
                .subject("expired-token-user")
                .audience(List.of("we-are-navodayan-api"))
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .id("expired-security-test-token")
                .claim("role", "USER")
                .build();

        String expiredToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        expiredClaims
                )
        ).getTokenValue();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsTokenWithWrongAudience() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("we-are-navodayan-auth")
                .subject("wrong-audience-user")
                .audience(List.of("another-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .id("wrong-audience-security-test-token")
                .claim("role", "USER")
                .build();

        String wrongAudienceToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongAudienceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsTokenWithWrongIssuer() throws Exception {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("another-auth-service")
                .subject("wrong-issuer-user")
                .audience(List.of("we-are-navodayan-api"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .id("wrong-issuer-security-test-token")
                .claim("role", "USER")
                .build();

        String wrongIssuerToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + wrongIssuerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpointRemainsPublic() throws Exception {
        String body = """
                {
                  "email": "security.test@example.com",
                  "password": "SecurePassword123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}