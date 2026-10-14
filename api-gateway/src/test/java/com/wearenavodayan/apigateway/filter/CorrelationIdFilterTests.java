package com.wearenavodayan.apigateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTests {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
    }

    @Test
    void shouldPreserveValidIncomingCorrelationId() throws Exception {
        String correlationId = "request-123";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", correlationId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<HttpServletRequest> forwardedRequest =
                new AtomicReference<>();

        FilterChain chain = (req, res) ->
                forwardedRequest.set((HttpServletRequest) req);

        filter.doFilter(request, response, chain);

        assertEquals(correlationId,
                response.getHeader("X-Correlation-ID"));
        assertEquals(correlationId,
                forwardedRequest.get().getHeader("X-Correlation-ID"));
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<HttpServletRequest> forwardedRequest =
                new AtomicReference<>();

        FilterChain chain = (req, res) ->
                forwardedRequest.set((HttpServletRequest) req);

        filter.doFilter(request, response, chain);

        String correlationId = response.getHeader("X-Correlation-ID");

        assertNotNull(correlationId);
        assertTrue(correlationId.matches(
                "[0-9a-fA-F-]{36}"));
        assertEquals(correlationId,
                forwardedRequest.get().getHeader("X-Correlation-ID"));
    }

    @Test
    void shouldReplaceInvalidIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "invalid id with spaces");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String correlationId = response.getHeader("X-Correlation-ID");

        assertNotNull(correlationId);
        assertTrue(correlationId.matches("[0-9a-fA-F-]{36}"));
        assertTrue(!correlationId.equals("invalid id with spaces"));
    }
}