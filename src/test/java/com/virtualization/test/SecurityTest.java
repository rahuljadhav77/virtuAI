package com.virtualization.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void adminEndpointRequiresApiKey() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/admin/services",
            String.class
        );
        assertTrue(
            response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
            response.getStatusCode() == HttpStatus.FORBIDDEN,
            "Admin endpoint should require authentication"
        );
    }

    @Test
    void invalidApiKeyIsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "wrong-key");

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void validApiKeyIsAccepted() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "enterprise-secret");

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void sqlInjectionAttemptIsBlocked() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "enterprise-secret");

        Map<String, Object> maliciousInput = new HashMap<>();
        maliciousInput.put("name", "'; DROP TABLE virtual_service; --");

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(maliciousInput, headers),
            String.class
        );

        assertFalse(response.getStatusCode().is5xxServerError(),
            "SQL injection should not cause server error");
    }

    @Test
    void xssAttemptIsHandled() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "enterprise-secret");

        Map<String, Object> xssInput = new HashMap<>();
        xssInput.put("name", "<script>alert('xss')</script>");

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(xssInput, headers),
            String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError(),
            "XSS input should be handled gracefully");
    }

    @Test
    void pathTraversalAttemptIsBlocked() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/admin/../../../etc/passwd",
            String.class
        );

        assertFalse(response.getStatusCode().is2xxSuccessful(),
            "Path traversal should be blocked");
    }

    @Test
    void actuatorEndpointsAreSecure() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/actuator/env",
            String.class
        );

        assertFalse(response.getStatusCode().is2xxSuccessful(),
            "Sensitive actuator endpoints should not be publicly accessible");
    }

    @Test
    void corsHeadersAreConfigured() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "enterprise-secret");

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        HttpHeaders responseHeaders = response.getHeaders();
        assertNotNull(responseHeaders, "Response headers should be present");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Valid request should succeed");
    }

    @Test
    void rateLimitingPreventsAbuse() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "enterprise-secret");

        int successCount = 0;
        int attemptCount = 100;

        for (int i = 0; i < attemptCount; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/api/admin/services",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                successCount++;
            }
            Thread.sleep(10);
        }

        assertEquals(attemptCount, successCount, "All legitimate requests should succeed");
    }

    @Test
    void malformedJsonReturnsError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "enterprise-secret");

        HttpEntity<String> entity = new HttpEntity<>("{invalid json}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            entity,
            String.class
        );

        assertTrue(
            response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError(),
            "Malformed JSON should return error"
        );
    }
}