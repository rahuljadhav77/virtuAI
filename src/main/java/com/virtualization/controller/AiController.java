package com.virtualization.controller;

import com.virtualization.service.GemmaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final GemmaService gemmaService;

    public AiController(GemmaService gemmaService) {
        this.gemmaService = gemmaService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean available = gemmaService.isAvailable();
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "AI service is ready" : "AI service not available. Ensure Ollama is running."
        ));
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String systemPrompt = request.get("systemPrompt");
        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        try {
            String response = gemmaService.chat(systemPrompt, message);
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            log.error("AI chat error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-mock")
    public ResponseEntity<?> generateMock(@RequestBody Map<String, String> request) {
        String description = request.get("description");

        if (description == null || description.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description is required"));
        }

        try {
            String mockJson = gemmaService.generateMockFromDescription(description);
            return ResponseEntity.ok(Map.of("mock", mockJson));
        } catch (Exception e) {
            log.error("Mock generation error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/generate-schema")
    public ResponseEntity<?> generateSchema(@RequestBody Map<String, String> request) {
        String payload = request.get("payload");

        if (payload == null || payload.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payload is required"));
        }

        try {
            String schema = gemmaService.generateSchemaFromPayload(payload);
            return ResponseEntity.ok(Map.of("schema", schema));
        } catch (Exception e) {
            log.error("Schema generation error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/analyze-traffic")
    public ResponseEntity<?> analyzeTraffic(@RequestBody Map<String, String> request) {
        String requestBody = request.get("requestBody");
        String responseBody = request.get("responseBody");

        try {
            String analysis = gemmaService.analyzeTrafficPattern(
                    requestBody != null ? requestBody : "",
                    responseBody != null ? responseBody : ""
            );
            return ResponseEntity.ok(Map.of("analysis", analysis));
        } catch (Exception e) {
            log.error("Traffic analysis error", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}