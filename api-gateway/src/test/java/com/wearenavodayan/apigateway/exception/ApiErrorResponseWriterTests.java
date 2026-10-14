package com.wearenavodayan.apigateway.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiErrorResponseWriterTests {

    private final ApiErrorResponseWriter writer =
            new ApiErrorResponseWriter(new ObjectMapper());

    @Test
    void shouldWriteJsonErrorResponseWithCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users/unknown");
        request.addHeader("X-Correlation-ID", "test-correlation-id");

        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(
                request,
                response,
                HttpServletResponse.SC_NOT_FOUND,
                "Not Found",
                "The requested API endpoint was not found.");

        String body = response.getContentAsString();

        assertEquals(404, response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));
        assertTrue(body.contains("\"status\":404"));
        assertTrue(body.contains("\"error\":\"Not Found\""));
        assertTrue(body.contains("/api/v1/users/unknown"));
        assertTrue(body.contains("test-correlation-id"));
    }

    @Test
    void shouldUseResponseCorrelationIdWhenAvailable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/users");

        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setHeader("X-Correlation-ID", "gateway-correlation-id");

        writer.write(
                request,
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred.");

        String body = response.getContentAsString();

        assertEquals(500, response.getStatus());
        assertTrue(body.contains("gateway-correlation-id"));
        assertTrue(body.contains("An unexpected error occurred."));
    }
}