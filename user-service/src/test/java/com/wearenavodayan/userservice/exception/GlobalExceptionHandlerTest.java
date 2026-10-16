package com.wearenavodayan.userservice.exception;

import com.wearenavodayan.userservice.dto.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldReturn404ForMissingEntity() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleNotFound(
                        new EntityNotFoundException("Profile not found"));

        assertResponse(response, HttpStatus.NOT_FOUND, "Profile not found");
    }

    @Test
    void shouldReturn400ForInvalidArgument() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleBadRequest(
                        new IllegalArgumentException("Invalid profile"));

        assertResponse(response, HttpStatus.BAD_REQUEST, "Invalid profile");
    }

    @Test
    void shouldReturn409ForIllegalState() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleConflict(
                        new IllegalStateException("Profile conflict"));

        assertResponse(response, HttpStatus.CONFLICT, "Profile conflict");
    }

    @Test
    void shouldPreserveStatusFromResponseStatusException() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleResponseStatus(
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED, "Authentication required"));

        assertResponse(
                response, HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    @Test
    void shouldReturn500WithoutLeakingUnexpectedExceptionDetails() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnexpectedException(
                        new RuntimeException("Sensitive internal details"));

        assertResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }

    private void assertResponse(
            ResponseEntity<ApiResponse<Void>> response,
            HttpStatus expectedStatus,
            String expectedMessage) {

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().message()).isEqualTo(expectedMessage);
    }
}
