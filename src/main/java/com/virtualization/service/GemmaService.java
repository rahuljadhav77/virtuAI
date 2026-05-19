package com.virtualization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualization.config.AiConfig;
import com.virtualization.repository.VirtualServiceRepository;
import com.virtualization.repository.VirtualRuleRepository;
import com.virtualization.repository.TrafficLogRepository;
import com.virtualization.entity.VirtualServiceEntity;
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
    private final VirtualServiceRepository virtualServiceRepository;
    private final VirtualRuleRepository virtualRuleRepository;
    private final TrafficLogRepository trafficLogRepository;

    public GemmaService(AiConfig aiConfig,
                        VirtualServiceRepository virtualServiceRepository,
                        VirtualRuleRepository virtualRuleRepository,
                        TrafficLogRepository trafficLogRepository) {
        this.aiConfig = aiConfig;
        this.virtualServiceRepository = virtualServiceRepository;
        this.virtualRuleRepository = virtualRuleRepository;
        this.trafficLogRepository = trafficLogRepository;
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
                log.warn("Gemini API request failed with status code {}: {}. Falling back to local generator.", response.statusCode(), response.body());
                return getLocalFallbackResponse(userMessage);
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
            log.warn("Error communicating with AI service (falling back to local generator): {}", e.getMessage());
            return getLocalFallbackResponse(userMessage);
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

    private String getLocalFallbackResponse(String userMessage) {
        String msg = userMessage.trim().toLowerCase();

        if (userMessage.startsWith("Create a mock for:")) {
            String description = userMessage.substring("Create a mock for:".length()).trim();
            String method = "GET";
            if (description.toLowerCase().contains("post")) method = "POST";
            else if (description.toLowerCase().contains("put")) method = "PUT";
            else if (description.toLowerCase().contains("delete")) method = "DELETE";

            String path = "/api/mock-service";
            if (description.contains("/")) {
                int slashIdx = description.indexOf("/");
                int spaceIdx = description.indexOf(" ", slashIdx);
                if (spaceIdx > slashIdx) {
                    path = description.substring(slashIdx, spaceIdx);
                } else {
                    path = description.substring(slashIdx);
                }
            }
            
            return String.format("""
                {
                  "name": "Local Mock for %s",
                  "method": "%s",
                  "pathPattern": "%s",
                  "responseBody": "{\\n  \\\"status\\\": \\\"success\\\",\\n  \\\"message\\\": \\\"Mock response generated locally for: %s\\\"\\n}",
                  "statusCode": 200,
                  "priority": 1
                }
                """, description.replace("\"", "\\\""), method, path, description.replace("\"", "\\\""));
        } else if (userMessage.startsWith("Generate schema for:")) {
            return """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "status": { "type": "string" },
                    "message": { "type": "string" }
                  }
                }
                """;
        } else if (userMessage.startsWith("Request:")) {
            return """
                {
                  "name": "Suggested Traffic Pattern",
                  "pathPattern": "/api/v1/.*",
                  "jsonPathCondition": "$.status",
                  "suggestions": "Consider verifying response status codes and schema matches."
                }
                """;
        }

        // Check if the user query requests system status or RAG context details
        boolean wantsRag = msg.contains("status") || msg.contains("report") || msg.contains("inventory") || 
                           msg.contains("active") || msg.contains("rule") || msg.contains("traffic") || 
                           msg.contains("log") || msg.contains("service") || msg.contains("rag") || 
                           msg.contains("stat");

        String ragSummary = "";
        if (wantsRag) {
            long serviceCount = 0;
            long ruleCount = 0;
            long logCount = 0;
            StringBuilder servicesSummary = new StringBuilder();

            try {
                serviceCount = virtualServiceRepository.count();
                ruleCount = virtualRuleRepository.count();
                logCount = trafficLogRepository.count();

                List<VirtualServiceEntity> servicesList = virtualServiceRepository.findAll();
                if (servicesList.isEmpty()) {
                    servicesSummary.append("<div style='font-style:italic; color:var(--text-muted); margin-left:0.5rem;'>No virtual services configured.</div>");
                } else {
                    for (VirtualServiceEntity s : servicesList) {
                        String statusColor = s.isEnabled() ? "var(--success)" : "var(--error)";
                        String statusText = s.isEnabled() ? "● Running" : "○ Stopped";
                        servicesSummary.append(String.format(
                            "<div style='margin-bottom:0.25rem; display:flex; justify-content:between; align-items:center;'>" +
                            "  <span><b>%s</b> (%s)</span>" +
                            "  <span style='font-size:0.7rem; font-weight:600; color:%s;'>%s</span>" +
                            "</div>",
                            s.getName(), s.getType(), statusColor, statusText
                        ));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to compile RAG context details", e);
                servicesSummary.append("<div style='color:var(--error);'>Error fetching RAG context details.</div>");
            }

            ragSummary = String.format("""
                <div class="rag-context" style="margin-top: 1rem; padding: 0.75rem; background: rgba(255,255,255,0.03); border: 1px solid var(--glass-border); border-radius: 0.5rem; font-size: 0.8rem; line-height: 1.4;">
                  <div style="font-weight: 600; color: var(--primary); margin-bottom: 0.5rem; display:flex; align-items:center; gap:0.25rem;">
                    <span style="font-size:0.9rem;">👁</span> Live RAG Context (System Status)
                  </div>
                  <div style="display:grid; grid-template-columns: 1fr 1fr; gap:0.5rem; margin-bottom:0.75rem; background:rgba(0,0,0,0.15); padding:0.5rem; border-radius:0.25rem;">
                    <div>Services: <b>%d</b></div>
                    <div>Mock Rules: <b>%d</b></div>
                    <div style="grid-column: 1/-1;">Transactions Logged: <b>%d</b></div>
                  </div>
                  <div style="margin-top: 0.5rem; font-weight: 600; font-size:0.75rem; text-transform:uppercase; letter-spacing:0.05em; color:var(--text-muted); margin-bottom:0.25rem;">Service Inventory:</div>
                  %s
                </div>
                """, serviceCount, ruleCount, logCount, servicesSummary.toString());
        }

        // Chat conversation handler using the dynamic RAG context
        if (msg.contains("hi") || msg.contains("hello") || msg.contains("hey")) {
            String greeting = "Hello! I am Virtumate, your API Virtualization Assistant. How can I help you design, record, or mock your virtual APIs today?";
            if (wantsRag) {
                return greeting + "\n" + ragSummary;
            } else {
                return greeting + " <i>(To see a live system status and service inventory, ask me for a 'status report'!)</i>";
            }
        } else if (msg.contains("help") || msg.contains("create") || msg.contains("service") || msg.contains("mock")) {
            String helperText = "To create a virtual service: Click the '+ Create Service' button at the top right, enter a name, choose the protocol (REST, SOAP, MQ), and pick a creation method. You can start with an Empty Service and add rules manually, upload/paste an OpenAPI spec, bulk-import rules from a JSON array, or generate a mock automatically from a sample Request/Response pair. Let me know if you need any help writing mock rules!";
            if (wantsRag) {
                return helperText + "\n\nHere is your current live system status:\n" + ragSummary;
            } else {
                return helperText + " <i>(To see your live service inventory directly in this chat, ask me for a 'status report'!)</i>";
            }
        }

        if (wantsRag) {
            return "Here is your requested system status report:\n" + ragSummary;
        }

        return "I am here to assist you with the API Virtualization Platform. You can ask me how to create services, configure mock rules, structure JSON paths, troubleshoot API routes, or ask for a 'system status report'.";
    }
}