package com.wearenavodayan.authservice;

import java.time.Instant;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.wearenavodayan.authservice.entity.RefreshToken;
import com.wearenavodayan.authservice.entity.UserAccount;
import com.wearenavodayan.authservice.repository.RefreshTokenRepository;
import com.wearenavodayan.authservice.repository.UserAccountRepository;
import com.wearenavodayan.authservice.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccount account;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();

        account = new UserAccount();
        account.setEmail("refresh.test@example.com");
        account.setFullName("Refresh Test");
        account.setPasswordHash(
                passwordEncoder.encode("SecurePassword123!"));

        account = userAccountRepository.saveAndFlush(account);
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        JsonNode response = login();

        assertTrue(response.get("accessToken").asText().length() > 0);
        assertTrue(response.get("refreshToken").asText().length() > 0);
        org.junit.jupiter.api.Assertions.assertEquals(
                900, response.get("expiresIn").asLong());
        org.junit.jupiter.api.Assertions.assertEquals(
                604800, response.get("refreshTokenExpiresIn").asLong());
    }

    @Test
    void refreshRotatesTokenAndReturnsNewAccessToken() throws Exception {
        String oldRefreshToken = login().get("refreshToken").asText();

        JsonNode response = refresh(oldRefreshToken);

        assertTrue(response.get("accessToken").asText().length() > 0);
        assertTrue(response.get("refreshToken").asText().length() > 0);
        assertNotEquals(
                oldRefreshToken,
                response.get("refreshToken").asText());
    }

    @Test
    void rotatedRefreshTokenCannotBeReused() throws Exception {
        String oldRefreshToken = login().get("refreshToken").asText();

        refresh(oldRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "refreshToken", oldRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void invalidRefreshTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"unknown-refresh-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        String expiredRawToken = "expired-refresh-token-for-test";

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUserId(account.getId());
        expiredToken.setTokenHash(
                RefreshTokenService.hashToken(expiredRawToken));
        expiredToken.setExpiresAt(Instant.now().minusSeconds(60));
        expiredToken.setRevoked(false);

        refreshTokenRepository.saveAndFlush(expiredToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "refreshToken", expiredRawToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }

    private JsonNode login() throws Exception {
        String body = """
                {
                  "email": "refresh.test@example.com",
                  "password": "SecurePassword123!"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString());
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("refreshToken", refreshToken));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString());
    }
}