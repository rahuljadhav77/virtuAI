package com.virtualization.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.repository.VirtualRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TestScenarioService {

    private static final String SYSTEM_PROMPT = """
            You are a test engineer specializing in API testing.
            Generate comprehensive test scenarios in JSON format.
            Include happy path, edge cases, and error scenarios.
            """;

    private final AiService aiService;
    private final VirtualRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    public TestScenarioService(AiService aiService, VirtualRuleRepository ruleRepository, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
    }

    public String generateTestScenarios(Long serviceId, String testType) {
        List<VirtualRuleEntity> rules = ruleRepository.findByServiceId(serviceId);

        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No rules found for service: " + serviceId);
        }

        StringBuilder context = new StringBuilder();
        context.append("## Service Endpoints\n\n");
        for (VirtualRuleEntity rule : rules) {
            context.append("### ").append(rule.getMethod()).append(" ").append(rule.getPathPattern()).append("\n");
            context.append("- Name: ").append(rule.getName()).append("\n");
            context.append("- Expected Status: ").append(rule.getStatusCode()).append("\n");
            if (rule.getRequestSchema() != null) {
                context.append("- Request Schema: ").append(rule.getRequestSchema()).append("\n");
            }
            if (rule.getResponseBody() != null) {
                context.append("- Response Body: ").append(rule.getResponseBody()).append("\n");
            }
            context.append("\n");
        }

        String scenarioType = switch (testType != null ? testType.toLowerCase() : "all") {
            case "happy" -> "only happy path scenarios with valid requests";
            case "edge" -> "only edge cases and boundary conditions";
            case "error" -> "only error scenarios and failure cases";
            default -> "all types: happy path, edge cases, and error scenarios";
        };

        String userPrompt = """
                Generate %s for this API.

                ## Endpoints
                %s

                Please generate test scenarios in valid JSON format with this structure:
                {
                  "scenarios": [
                    {
                      "name": "Test case name",
                      "type": "happy|edge|error",
                      "description": "What this test validates",
                      "request": {
                        "method": "GET|POST|PUT|DELETE|PATCH",
                        "path": "/actual/path/to/test",
                        "headers": {},
                        "body": {}
                      },
                      "expected": {
                        "statusCode": 200,
                        "responseBodyContains": [],
                        "responseHeaders": {}
                      }
                    }
                  ]
                }

                Generate at least 3 scenarios of each type.
                Ensure test paths match the endpoint patterns.
                Include realistic test data.
                """.formatted(scenarioType, context);

        log.info("Generating {} test scenarios for service: {}", testType, serviceId);
        String response = aiService.generateCompletion(SYSTEM_PROMPT, userPrompt);

        return extractJsonFromResponse(response);
    }

    public String generateOpenApiTestScenarios(String openApiSpec, String testType) {
        String scenarioType = switch (testType != null ? testType.toLowerCase() : "all") {
            case "happy" -> "only happy path scenarios";
            case "edge" -> "only edge cases and boundary testing";
            case "error" -> "only error scenarios and negative testing";
            default -> "all scenarios: happy path, edge cases, and error scenarios";
        };

        String userPrompt = """
                Generate %s for this OpenAPI specification.

                ## OpenAPI Spec
                %s

                Generate test scenarios in valid JSON format:
                {
                  "scenarios": [
                    {
                      "name": "Descriptive test name",
                      "type": "happy|edge|error",
                      "description": "Test purpose",
                      "request": {
                        "method": "HTTP method",
                        "path": "/actual/test/path",
                        "headers": {},
                        "body": {}
                      },
                      "expected": {
                        "statusCode": 200,
                        "responseBodyContains": [],
                        "responseHeaders": {}
                      }
                    }
                  ]
                }

                Generate at least 5 scenarios covering different endpoints.
                Include parameter variations for edge cases.
                """.formatted(scenarioType, openApiSpec);

        log.info("Generating OpenAPI test scenarios, type: {}", testType);
        String response = aiService.generateCompletion(SYSTEM_PROMPT, userPrompt);

        return extractJsonFromResponse(response);
    }

    public List<Map<String, Object>> parseTestScenarios(String jsonResponse) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(jsonResponse, Map.class);
            return (List<Map<String, Object>>) parsed.getOrDefault("scenarios", new ArrayList<>());
        } catch (JsonProcessingException e) {
            log.warn("Could not parse AI response as JSON, returning raw text");
            return List.of(Map.of(
                    "raw", jsonResponse,
                    "format", "markdown",
                    "note", "AI response could not be parsed as structured JSON"
            ));
        }
    }

    private String extractJsonFromResponse(String response) {
        String trimmed = response.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        trimmed = trimmed.trim();

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                objectMapper.readTree(trimmed);
                return trimmed;
            } catch (JsonProcessingException e) {
                log.debug("Response doesn't appear to be pure JSON, wrapping in JSON structure");
            }
        }

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.put("raw", trimmed);
        wrapper.put("format", "markdown");
        wrapper.put("warning", "Response was not in expected JSON format");

        try {
            return objectMapper.writeValueAsString(wrapper);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Could not format response\"}";
        }
    }
}
