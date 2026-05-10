package com.virtualization.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertNotNull(restTemplate);
    }

    @Test
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    void actuatorMetricsEndpointAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    }

    @Test
    void mainPageReturnsOk() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    }

    @Test
    void nonExistentEndpointReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity("/nonexistent", String.class);
        assertTrue(
            response.getStatusCode().value() == 404 ||
            response.getStatusCode().value() == 500 ||
            response.getStatusCode().value() == 400,
            "Non-existent endpoint should return error, got: " + response.getStatusCode()
        );
    }
}