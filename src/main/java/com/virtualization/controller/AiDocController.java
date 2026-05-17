package com.virtualization.controller;

import com.virtualization.service.GemmaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/docs")
public class AiDocController {

    private final GemmaService gemmaService;

    public AiDocController(GemmaService gemmaService) {
        this.gemmaService = gemmaService;
    }

    @PostMapping("/generate-test-cases")
    public ResponseEntity<?> generateTestCases(@RequestBody Map<String, String> request) {
        String mockRule = request.get("mockRule");
        if (mockRule == null || mockRule.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mock rule JSON is required"));
        }

        try {
            String systemPrompt = """
                You are an expert QA engineer. Given a mock rule definition in JSON format,
                generate comprehensive test cases that cover:
                - Happy path scenarios
                - Edge cases
                - Error conditions
                - Boundary conditions

                Return as a JSON array of test case objects with: name, description, request details, expected response
                Return ONLY valid JSON array, no explanation.
                """;

            String testCases = gemmaService.chat(systemPrompt, "Generate test cases for this mock:\n" + mockRule);
            return ResponseEntity.ok(Map.of("testCases", testCases));
        } catch (Exception e) {
            log.error("Test case generation error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-documentation")
    public ResponseEntity<?> generateDocumentation(@RequestBody Map<String, String> request) {
        String mockRule = request.get("mockRule");
        if (mockRule == null || mockRule.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mock rule JSON is required"));
        }

        try {
            String systemPrompt = """
                You are a technical writer. Given a mock rule definition in JSON format,
                generate comprehensive API documentation including:
                - Endpoint overview
                - Request format and parameters
                - Response format and status codes
                - Example requests and responses
                - Usage notes and tips

                Return as well-formatted Markdown, no explanation.
                """;

            String docs = gemmaService.chat(systemPrompt, "Generate documentation for this mock:\n" + mockRule);
            return ResponseEntity.ok(Map.of("documentation", docs));
        } catch (Exception e) {
            log.error("Documentation generation error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}