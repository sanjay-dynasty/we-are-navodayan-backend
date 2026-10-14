package com.wearenavodayan.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ServiceCommunicationConfigurationTests {

    @Autowired
    private Environment environment;

    @Test
    void shouldConfigureDownstreamConnectionTimeout() {
        assertEquals(
                "3s",
                environment.getProperty("spring.http.client.connect-timeout"));
    }

    @Test
    void shouldConfigureDownstreamReadTimeout() {
        assertEquals(
                "10s",
                environment.getProperty("spring.http.client.read-timeout"));
    }

    @Test
    void shouldKeepDownstreamServiceUrlsConfigured() {
        assertEquals(
                "http://localhost:8081",
                environment.getProperty("services.auth.base-url"));
        assertEquals(
                "http://localhost:8082",
                environment.getProperty("services.user.base-url"));
        assertEquals(
                "http://localhost:8083",
                environment.getProperty("services.jnv.base-url"));
    }
}