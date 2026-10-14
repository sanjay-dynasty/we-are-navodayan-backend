package com.wearenavodayan.authservice;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.wearenavodayan.authservice.entity.UserAccount;
import com.wearenavodayan.authservice.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthRegistrationIntegrationTests {

    private static final String REGISTER_URL = "/api/v1/auth/register";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String testEmail;

    @AfterEach
    void cleanUp() {
        if (testEmail != null) {
            userAccountRepository.findByEmail(testEmail)
                    .ifPresent(userAccountRepository::delete);
        }
    }

    @Test
    void registerValidUserReturnsCreatedAndStoresBcryptHash() throws Exception {
        testEmail = "integration-" + UUID.randomUUID() + "@example.com";
        String rawPassword = "SecureTestPassword123";

        String requestBody = """
                {
                  "fullName": "Integration Test User",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(testEmail, rawPassword);

        String responseBody = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);

        assertTrue(response.path("success").asBoolean());
        assertEquals("User registered successfully",
                response.path("message").asText());
        assertEquals(testEmail, response.path("data").path("email").asText());
        assertEquals("Integration Test User",
                response.path("data").path("fullName").asText());
        assertEquals("USER", response.path("data").path("role").asText());

        assertFalse(responseBody.contains("passwordHash"));
        assertFalse(responseBody.contains(rawPassword));

        UserAccount savedUser = userAccountRepository.findByEmail(testEmail)
                .orElseThrow();

        assertNotEquals(rawPassword, savedUser.getPasswordHash());
        assertEquals(60, savedUser.getPasswordHash().length());
        assertTrue(passwordEncoder.matches(
                rawPassword, savedUser.getPasswordHash()));
    }

    @Test
    void registerDuplicateEmailReturnsConflict() throws Exception {
        testEmail = "integration-" + UUID.randomUUID() + "@example.com";

        String requestBody = """
                {
                  "fullName": "Integration Test User",
                  "email": "%s",
                  "password": "SecureTestPassword123"
                }
                """.formatted(testEmail);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void registerInvalidEmailReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "fullName": "Integration Test User",
                  "email": "not-an-email",
                  "password": "SecureTestPassword123"
                }
                """;

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShortPasswordReturnsBadRequest() throws Exception {
        String requestBody = """
                {
                  "fullName": "Integration Test User",
                  "email": "integration-%s@example.com",
                  "password": "short"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}