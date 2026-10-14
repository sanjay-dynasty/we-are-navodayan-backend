package com.wearenavodayan.apigateway.filter;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RateLimitFilterTests {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(2, 60);
    }

    @Test
    void shouldAllowRequestsWithinLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("GET", "/api/v1/users");
            MockHttpServletResponse response =
                    new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus());
            org.mockito.Mockito.verify(chain).doFilter(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void shouldRejectRequestsBeyondLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest request =
                    new MockHttpServletRequest("GET", "/api/v1/users");
            MockHttpServletResponse response =
                    new MockHttpServletResponse();

            filter.doFilter(request, response, (req, res) -> {});
        }

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/users");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertEquals(429, response.getStatus());
        assertEquals("2", response.getHeader("X-RateLimit-Limit"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
        assertTrue(response.getHeader("Retry-After") != null);
    }

    @Test
    void shouldSkipCorsPreflightRequests() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("OPTIONS", "/api/v1/users");
        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertEquals(200, response.getStatus());
    }
}