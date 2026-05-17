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
        if (aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()) {
            log.warn("AI service not available: API key is missing");
            return false;
        }
        return true;
    }

    public String chat(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            throw new RuntimeException("AI service is not available. Please provide a Google Gemini API Key.");
        }

        try {
            Map<String, Object> requestBody = new HashMap<>();
            
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                requestBody.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
                ));
            }
            
            requestBody.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", userMessage)))
            ));

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            String model = aiConfig.getModel();
            if (model == null || model.isBlank() || model.contains("gemma") || model.equals("gemma:2b")) {
                model = "gemini-2.5-flash"; // Fallback to Gemini 2.5 Flash
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + aiConfig.getApiKey();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("AI request failed: " + response.statusCode() + " - " + response.body());
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            
            if (jsonResponse.has("candidates") && jsonResponse.get("candidates").size() > 0) {
                JsonNode firstCandidate = jsonResponse.get("candidates").get(0);
                if (firstCandidate.has("content") && firstCandidate.get("content").has("parts")) {
                     return firstCandidate.get("content").get("parts").get(0).get("text").asText();
                }
            }
            
            throw new RuntimeException("Unexpected response format from Gemini API");

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