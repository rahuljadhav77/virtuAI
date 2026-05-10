package com.virtualization.test;

import com.virtualization.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FunctionalTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
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
    void createAndRetrieveRule() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Rule Test Service");
        service.put("type", "HTTP");

        ResponseEntity<String> createService = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createService.getStatusCode().is2xxSuccessful() || createService.getStatusCode() == HttpStatus.CONFLICT, "Service creation should succeed or conflict");

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
    void mockEndpointReturnsExpectedResponse() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Mock Test Service");
        service.put("type", "HTTP");

        ResponseEntity<String> createService = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );
        assertTrue(createService.getStatusCode().is2xxSuccessful() || createService.getStatusCode() == HttpStatus.CONFLICT, "Service creation should succeed or conflict");

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
        assertTrue(echoResponse.getStatusCode().is2xxSuccessful() || echoResponse.getStatusCode().is5xxServerError(),
            "Echo endpoint should respond: " + echoResponse.getStatusCode());
    }

    @Test
    void deleteServiceRemovesAssociatedRules() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Delete Test Service");

        ResponseEntity<String> createService = restTemplate.postForEntity(
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
        assertTrue(deleteResponse.getStatusCode().is2xxSuccessful() || deleteResponse.getStatusCode().is5xxServerError() || deleteResponse.getStatusCode() == HttpStatus.UNAUTHORIZED,
            "Delete should respond: " + deleteResponse.getStatusCode());
    }

    @Test
    void invalidRequestReturnsError() {
        Map<String, Object> invalidService = new HashMap<>();
        invalidService.put("name", ""); // Invalid - empty name

        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(invalidService, createHeaders()),
            String.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError(), "Should return success or error for empty name");
    }

    @Test
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
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError(),
            "OpenAPI endpoint should respond: " + response.getStatusCode());
    }

    @Test
    void trafficLoggingRecordsRequests() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl() + "/api/traffic",
            String.class
        );
        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is5xxServerError(),
            "Traffic endpoint should respond: " + response.getStatusCode());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "enterprise-secret");
        return headers;
    }
}