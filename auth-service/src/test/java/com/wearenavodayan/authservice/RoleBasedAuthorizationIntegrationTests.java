package com.wearenavodayan.authservice;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class RoleBasedAuthorizationIntegrationTests {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private JwtEncoder jwtEncoder;

        @Test
        void adminEndpointRejectsMissingToken() throws Exception {
                mockMvc.perform(get("/api/v1/admin/access"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                                .andExpect(jsonPath("$.path").value("/api/v1/admin/access"));
        }

        @Test
        void adminEndpointRejectsUserRole() throws Exception {
                String token = createToken("USER");

                mockMvc.perform(get("/api/v1/admin/access")
                                .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + token))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                                .andExpect(jsonPath("$.message").isNotEmpty())
                                .andExpect(jsonPath("$.path").value("/api/v1/admin/access"));
        }

        @Test
        void adminEndpointReturnsStandardErrorForInvalidToken()
                        throws Exception {
                mockMvc.perform(get("/api/v1/admin/access")
                                .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer invalid-token"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                                .andExpect(jsonPath("$.path").value("/api/v1/admin/access"));
        }

        @Test
        void adminEndpointAcceptsAdminRole() throws Exception {
                String token = createToken("ADMIN");

                mockMvc.perform(get("/api/v1/admin/access")
                                .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(content().json(
                                                """
                                                                {
                                                                  "message": "Admin access granted"
                                                                }
                                                                """));
        }

        @Test
        void authenticatedUserCanAccessCurrentUserEndpoint()
                        throws Exception {
                String token = createToken("USER");

                mockMvc.perform(get("/api/v1/auth/me")
                                .header(
                                                HttpHeaders.AUTHORIZATION,
                                                "Bearer " + token))
                                .andExpect(status().isOk());
        }

        private String createToken(String role) {
                Instant now = Instant.now();

                JwtClaimsSet claims = JwtClaimsSet.builder()
                                .issuer("we-are-navodayan-auth")
                                .subject(UUID.randomUUID().toString())
                                .audience(List.of("we-are-navodayan-api"))
                                .issuedAt(now)
                                .expiresAt(now.plusSeconds(900))
                                .id(UUID.randomUUID().toString())
                                .claim("role", role)
                                .build();

                return jwtEncoder.encode(
                                JwtEncoderParameters.from(
                                                JwsHeader.with(MacAlgorithm.HS256).build(),
                                                claims))
                                .getTokenValue();
        }
}