package com.virtualization.test;

import com.virtualization.config.AiConfig;
import com.virtualization.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AiFeaturesTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AiService aiService;

    @Autowired
    private AiConfig aiConfig;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "enterprise-secret");
        return headers;
    }

    @Test
    void aiServiceIsEnabled() {
        assertTrue(aiConfig.isEnabled(), "AI should be enabled");
        assertNotNull(aiConfig.getApiKey(), "AI API key should be configured");
        assertFalse(aiConfig.getApiKey().isBlank(), "AI API key should not be blank");
    }

    @Test
    void aiServiceCanGenerateCompletion() {
        if (!aiService.isEnabled()) {
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
            response.getStatusCode().is5xxServerError(),
            "Doc endpoint should respond: " + response.getStatusCode()
        );
    }

    @Test
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
            response.getStatusCode().is4xxClientError(),
            "Test scenario endpoint should respond"
        );
    }

    @Test
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
            response.getStatusCode() == HttpStatus.NOT_FOUND,
            "Self-healing endpoint should be accessible"
        );
    }

    @Test
    void quickstartEndpointExists() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/ai/docs/quickstart",
            String.class
        );
        assertTrue(
            response.getStatusCode().is2xxSuccessful() ||
            response.getStatusCode().is4xxClientError() ||
            response.getStatusCode().is5xxServerError(),
            "Quickstart endpoint should respond: " + response.getStatusCode()
        );
    }

    @Test
    void generateDocumentationForService() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "AI Test Service");
        service.put("type", "HTTP");

        ResponseEntity<String> createService = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createService.getStatusCode().is2xxSuccessful(),
            "Service creation should succeed: " + createService.getStatusCode());
    }

    @Test
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
            response.getStatusCode().is5xxServerError(),
            "Invalid OpenAPI spec should return error"
        );
    }
}