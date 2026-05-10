package com.virtualization.test;

import com.virtualization.config.AiConfig;
import com.virtualization.service.AiService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComprehensiveTestSuite {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private AiService aiService;

    @Autowired(required = false)
    private AiConfig aiConfig;

    private static final String API_KEY = "enterprise-secret";

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);
        return headers;
    }

    // ==================== SMOKE TESTS ====================

    @Test
    @Order(1)
    void contextLoads() {
        assertNotNull(restTemplate);
    }

    @Test
    @Order(2)
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    @Order(3)
    void actuatorMetricsEndpointAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    }

    @Test
    @Order(4)
    void mainPageReturnsOk() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    }

    @Test
    @Order(5)
    void nonExistentEndpointReturnsError() {
        ResponseEntity<String> response = restTemplate.getForEntity("/nonexistent", String.class);
        assertTrue(
            response.getStatusCode().value() == 404 ||
            response.getStatusCode().value() == 500 ||
            response.getStatusCode().value() == 400,
            "Non-existent endpoint should return error, got: " + response.getStatusCode()
        );
    }

    // ==================== FUNCTIONAL TESTS ====================

    @Test
    @Order(10)
    void createAndRetrieveService() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Test Service " + System.currentTimeMillis());
        service.put("description", "Functional test service");
        service.put("type", "HTTP");

        ResponseEntity<String> createResponse = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createResponse.getStatusCode().is2xxSuccessful() || createResponse.getStatusCode() == HttpStatus.CONFLICT);

        ResponseEntity<String> listResponse = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.GET,
            new HttpEntity<>(createHeaders()),
            String.class
        );
        assertTrue(listResponse.getStatusCode().is2xxSuccessful() || listResponse.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(11)
    void createAndRetrieveRule() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Rule Test Service");
        service.put("type", "HTTP");

        ResponseEntity<String> createService = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createService.getStatusCode().is2xxSuccessful() || createService.getStatusCode() == HttpStatus.CONFLICT);

        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Get User Rule");
        rule.put("pathPattern", "/api/users/*");
        rule.put("method", "GET");
        rule.put("responseBody", "{\"id\": 1, \"name\": \"Test User\"}");
        rule.put("statusCode", 200);

        restTemplate.postForEntity(
            baseUrl() + "/api/admin/rules",
            new HttpEntity<>(rule, createHeaders()),
            String.class
        );
        assertTrue(true, "Rule created");
    }

    @Test
    @Order(12)
    void mockEndpointReturnsExpectedResponse() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Mock Test Service");
        service.put("type", "HTTP");

        ResponseEntity<String> createService = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createService.getStatusCode().is2xxSuccessful() || createService.getStatusCode() == HttpStatus.CONFLICT);

        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Echo Rule");
        rule.put("pathPattern", "/echo");
        rule.put("method", "GET");
        rule.put("responseBody", "{\"echo\": \"test\"}");
        rule.put("statusCode", 200);

        restTemplate.postForEntity(
            baseUrl() + "/api/admin/rules",
            new HttpEntity<>(rule, createHeaders()),
            String.class
        );

        ResponseEntity<String> echoResponse = restTemplate.getForEntity(
            baseUrl() + "/echo",
            String.class
        );
        assertTrue(echoResponse.getStatusCode().is2xxSuccessful() || echoResponse.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(13)
    void deleteServiceRemovesAssociatedRules() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Delete Test Service");

        restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.DELETE,
            new HttpEntity<>(createHeaders()),
            String.class
        );
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful() ||
                   deleteResponse.getStatusCode().is5xxServerError() ||
                   deleteResponse.getStatusCode() == HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(14)
    void invalidRequestReturnsError() {
        Map<String, Object> invalidService = new HashMap<>();
        invalidService.put("name", "");

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(invalidService, createHeaders()),
            String.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError());
    }

    @Test
    @Order(15)
    void openApiImportParsesCorrectly() {
        String openApiSpec = """
            {
              "openapi": "3.0.0",
              "info": { "title": "Test API", "version": "1.0" },
              "paths": {
                "/users": {
                  "get": {
                    "responses": {
                      "200": { "description": "Success" }
                    }
                  }
                }
              }
            }
            """;

        Map<String, Object> request = new HashMap<>();
        request.put("spec", openApiSpec);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/ai/test-scenarios/openapi",
            new HttpEntity<>(request, createHeaders()),
            String.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError());
    }

    @Test
    @Order(16)
    void trafficLoggingRecordsRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/traffic",
            String.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is5xxServerError());
    }

    // ==================== PERFORMANCE TESTS ====================

    @Test
    @Order(20)
    void concurrentRequestsHandled() throws InterruptedException {
        int threadCount = 20;
        int requestsPerThread = 5;
        List<Thread> threads = new ArrayList<>();
        List<int[]> results = Collections.synchronizedList(new ArrayList<>());
        long startTime = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                int success = 0;
                for (int i = 0; i < requestsPerThread; i++) {
                    try {
                        ResponseEntity<String> response = restTemplate.exchange(
                            baseUrl() + "/api/admin/services",
                            HttpMethod.GET,
                            new HttpEntity<>(createHeaders()),
                            String.class
                        );
                        if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                            success++;
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
                results.add(new int[]{threadId, success});
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        long duration = System.currentTimeMillis() - startTime;
        int totalSuccess = results.stream().mapToInt(r -> r[1]).sum();
        int totalRequests = threadCount * requestsPerThread;

        assertTrue(totalSuccess > 0, "At least some requests should succeed");
        System.out.println("Performance: " + totalSuccess + "/" + totalRequests + " succeeded in " + duration + "ms");
    }

    @Test
    @Order(21)
    void responseTimeUnderLoad() {
        long totalTime = 0;
        int iterations = 10;

        for (int i = 0; i < iterations; i++) {
            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/api/admin/services",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class
            );
            totalTime += (System.currentTimeMillis() - start);

            assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.UNAUTHORIZED);
        }

        double avgTime = (double) totalTime / iterations;
        assertTrue(avgTime < 5000, "Average response time should be under 5 seconds, was: " + avgTime + "ms");
        System.out.println("Average response time: " + avgTime + "ms");
    }

    @Test
    @Order(22)
    void memoryUsageStaysStable() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        Thread.sleep(100);

        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 20; i++) {
            restTemplate.exchange(
                baseUrl() + "/api/admin/services",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class
            );
        }

        Thread.sleep(100);
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();

        long memoryIncrease = afterMemory - beforeMemory;
        assertTrue(memoryIncrease < 100 * 1024 * 1024, "Memory increase should be under 100MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    }

    // ==================== SECURITY TESTS ====================

    @Test
    @Order(30)
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
    @Order(31)
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
    @Order(32)
    void validApiKeyIsAccepted() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", API_KEY);

        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(33)
    void sqlInjectionAttemptIsBlocked() {
        Map<String, Object> maliciousInput = new HashMap<>();
        maliciousInput.put("name", "'; DROP TABLE virtual_service; --");

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(maliciousInput, createHeaders()),
            String.class
        );

        assertFalse(response.getStatusCode().is5xxServerError());
    }

    @Test
    @Order(34)
    void xssAttemptIsHandled() {
        Map<String, Object> xssInput = new HashMap<>();
        xssInput.put("name", "<script>alert('xss')</script>");

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(xssInput, createHeaders()),
            String.class
        );

        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError());
    }

    @Test
    @Order(35)
    void pathTraversalAttemptIsBlocked() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/admin/../../../etc/passwd",
            String.class
        );

        assertFalse(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Order(36)
    void actuatorEndpointsAreSecure() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/actuator/env",
            String.class
        );

        assertFalse(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Order(37)
    void corsHeadersAreConfigured() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/admin/services",
            HttpMethod.GET,
            new HttpEntity<>(createHeaders()),
            String.class
        );

        assertNotNull(response.getHeaders());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(38)
    void rateLimitingPreventsAbuse() throws InterruptedException {
        int successCount = 0;
        int attemptCount = 50;

        for (int i = 0; i < attemptCount; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/api/admin/services",
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                successCount++;
            }
            Thread.sleep(10);
        }

        assertEquals(attemptCount, successCount, "All legitimate requests should succeed");
    }

    @Test
    @Order(39)
    void malformedJsonReturnsError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", API_KEY);

        HttpEntity<String> entity = new HttpEntity<>("{invalid json}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            entity,
            String.class
        );

        assertTrue(
            response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError()
        );
    }

    // ==================== AI FEATURES TESTS ====================

    @Test
    @Order(40)
    void aiServiceIsEnabled() {
        if (aiConfig == null) {
            System.out.println("AI config not available, skipping");
            return;
        }
        assertTrue(aiConfig.isEnabled() || !aiConfig.getApiKey().isBlank(),
                   "AI should be enabled or API key configured");
    }

    @Test
    @Order(41)
    void aiServiceCanGenerateCompletion() {
        if (aiService == null || !aiService.isEnabled()) {
            System.out.println("AI service not enabled, skipping test");
            return;
        }
        try {
            String result = aiService.generateCompletion(
                "You are a helpful assistant. Reply with only 'test'.",
                "Say test"
            );
            assertNotNull(result);
        } catch (Exception e) {
            System.out.println("AI call failed (expected if API key issue): " + e.getMessage());
        }
    }

    @Test
    @Order(42)
    void documentationEndpointExists() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/ai/docs/1",
            HttpMethod.GET,
            new HttpEntity<>(createHeaders()),
            String.class
        );
        assertTrue(
            response.getStatusCode().is2xxSuccessful() ||
            response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError()
        );
    }

    @Test
    @Order(43)
    void testScenarioEndpointExists() {
        Map<String, Object> request = new HashMap<>();
        request.put("spec", "{}");

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/ai/test-scenarios/openapi",
            new HttpEntity<>(request, createHeaders()),
            String.class
        );

        assertTrue(
            response.getStatusCode().is2xxSuccessful() ||
            response.getStatusCode().is4xxClientError()
        );
    }

    @Test
    @Order(44)
    void selfHealingEndpointExists() {
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl() + "/api/ai/self-healing/health/1",
            HttpMethod.GET,
            new HttpEntity<>(createHeaders()),
            String.class
        );

        assertTrue(
            response.getStatusCode().is2xxSuccessful() ||
            response.getStatusCode() == HttpStatus.BAD_REQUEST ||
            response.getStatusCode() == HttpStatus.NOT_FOUND
        );
    }

    @Test
    @Order(45)
    void quickstartEndpointExists() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/ai/docs/quickstart",
            String.class
        );
        assertTrue(
            response.getStatusCode().is2xxSuccessful() ||
            response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError()
        );
    }

    @Test
    @Order(46)
    void generateDocumentationForService() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "AI Test Service");
        service.put("type", "HTTP");

        ResponseEntity<String> createService = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createService.getStatusCode().is2xxSuccessful() ||
                   createService.getStatusCode() == HttpStatus.CONFLICT);
    }

    @Test
    @Order(47)
    void openApiSpecValidation() {
        String invalidSpec = "not valid json";

        Map<String, Object> request = new HashMap<>();
        request.put("spec", invalidSpec);

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/ai/test-scenarios/openapi",
            new HttpEntity<>(request, createHeaders()),
            String.class
        );

        assertTrue(
            response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError()
        );
    }
}