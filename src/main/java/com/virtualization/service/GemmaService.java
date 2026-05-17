package com.virtualization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualization.config.AiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class GemmaService {
    private final AiConfig aiConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GemmaService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isAvailable() {
        if (!aiConfig.isEnabled()) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiConfig.getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("AI service not available: {}", e.getMessage());
            return false;
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            throw new RuntimeException("AI service is not available. Please ensure Ollama is running at " + aiConfig.getBaseUrl());
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiConfig.getBaseUrl() + "/api/chat"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("AI request failed: " + response.statusCode() + " - " + response.body());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            return jsonResponse.get("message").get("content").asText();

        } catch (Exception e) {
            log.error("Error communicating with AI service", e);
            throw new RuntimeException("AI communication error: " + e.getMessage());
        }
    }

    public String generateMockFromDescription(String description) {
        String systemPrompt = """
            You are an expert API mock generator. Given a natural language description of an API endpoint,
            respond with a JSON object that defines a VirtualRuleEntity with these fields:
            - name: descriptive name for the rule
            - method: HTTP method (GET, POST, PUT, DELETE)
            - pathPattern: regex pattern for the path
            - responseBody: sample JSON response body
            - statusCode: HTTP status code (default 200)
            - priority: rule priority (default 1)

            Return ONLY valid JSON, no explanation.
            """;

        String userMessage = "Create a mock for: " + description;
        return chat(systemPrompt, userMessage);
    }

    public String generateSchemaFromPayload(String payload) {
        String systemPrompt = """
            You are a JSON Schema generator. Given a JSON payload, analyze its structure
            and generate a JSON Schema that can validate similar payloads.

            Return ONLY a valid JSON Schema object, no explanation.
            """;

        String userMessage = "Generate schema for: " + payload;
        return chat(systemPrompt, userMessage);
    }

    public String analyzeTrafficPattern(String requestBody, String responseBody) {
        String systemPrompt = """
            You are an API virtualization expert. Analyze the provided request and response bodies.
            Suggest:
            1. A good name for this mock rule
            2. A regex path pattern if applicable
            3. Any JSON path conditions that would help match this request
            4. Suggested response body improvements

            Return as a JSON object with keys: name, pathPattern, jsonPathCondition, suggestions
            """;

        String userMessage = "Request: " + requestBody + "\nResponse: " + responseBody;
        return chat(systemPrompt, userMessage);
    }
}