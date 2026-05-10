package com.virtualization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.virtualization.config.AiConfig;
import com.virtualization.exception.ImportValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class AiService {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiConfig aiConfig;
    private final HttpClient httpClient;

    public AiService(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String generateCompletion(String systemPrompt, String userPrompt) {
        if (!aiConfig.isEnabled()) {
            throw new ImportValidationException("AI features are disabled. Please configure ai.api-key in application.properties");
        }

        if (aiConfig.getApiKey() == null || aiConfig.getApiKey().isBlank()) {
            throw new ImportValidationException("AI API key not configured. Please set ai.api-key in application.properties");
        }

        try {
            return switch (aiConfig.getProvider().toLowerCase()) {
                case "openai" -> callOpenAi(systemPrompt, userPrompt);
                case "gemini" -> callGemini(systemPrompt, userPrompt);
                default -> throw new ImportValidationException("Unsupported AI provider: " + aiConfig.getProvider());
            };
        } catch (ImportValidationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling AI service", e);
            throw new ImportValidationException("Failed to call AI service: " + e.getMessage());
        }
    }

    private String callGemini(String systemPrompt, String userPrompt) throws Exception {
        String combinedPrompt = systemPrompt + "\n\n" + userPrompt;

        ObjectNode requestBody = MAPPER.createObjectNode();
        requestBody.put("contents", createContentsArray(combinedPrompt));

        ObjectNode generationConfig = MAPPER.createObjectNode();
        generationConfig.put("temperature", aiConfig.getTemperature());
        generationConfig.put("maxOutputTokens", aiConfig.getMaxTokens());
        requestBody.set("generationConfig", generationConfig);

        String url = String.format("%s/models/%s:generateContent?key=%s",
                aiConfig.getBaseUrl(), aiConfig.getModel(), aiConfig.getApiKey());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
            throw new ImportValidationException("AI API error: " + response.statusCode());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String content = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();

        if (content == null || content.isBlank()) {
            throw new ImportValidationException("AI returned empty response");
        }

        return content;
    }

    private ArrayNode createContentsArray(String prompt) {
        ArrayNode contents = MAPPER.createArrayNode();
        ObjectNode part = MAPPER.createObjectNode();
        part.put("text", prompt);
        ObjectNode content = MAPPER.createObjectNode();
        content.set("parts", MAPPER.createArrayNode().add(part));
        contents.add(content);
        return contents;
    }

    private String callOpenAi(String systemPrompt, String userPrompt) throws Exception {
        ObjectNode requestBody = MAPPER.createObjectNode();
        requestBody.put("model", aiConfig.getModel());

        ArrayNode messages = MAPPER.createArrayNode();
        ObjectNode systemMsg = MAPPER.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        ObjectNode userMsg = MAPPER.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestBody.set("messages", messages);
        requestBody.put("temperature", aiConfig.getTemperature());
        requestBody.put("max_tokens", aiConfig.getMaxTokens());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiConfig.getBaseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiConfig.getApiKey())
                .timeout(Duration.ofSeconds(aiConfig.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("OpenAI API error: {} - {}", response.statusCode(), response.body());
            throw new ImportValidationException("AI API error: " + response.statusCode());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText();

        if (content == null || content.isBlank()) {
            throw new ImportValidationException("AI returned empty response");
        }

        return content;
    }

    public boolean isEnabled() {
        return aiConfig.isEnabled() &&
               aiConfig.getApiKey() != null &&
               !aiConfig.getApiKey().isBlank();
    }
}
