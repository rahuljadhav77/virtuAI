package com.virtualization.controller;

import com.virtualization.service.AiService;
import com.virtualization.service.ChatMemoryService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/chat")
public class ChatController {

    private final AiService aiService;
    private final ChatMemoryService chatMemoryService;

    public ChatController(AiService aiService, ChatMemoryService chatMemoryService) {
        this.aiService = aiService;
        this.chatMemoryService = chatMemoryService;
    }

    @PostMapping("/message")
    public Map<String, Object> sendMessage(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "default");
        String message = request.get("message");
        boolean includeContext = Boolean.parseBoolean(request.getOrDefault("includeContext", "true"));
        int historyLimit = Integer.parseInt(request.getOrDefault("historyLimit", "10"));

        // Save user message
        chatMemoryService.addUserMessage(sessionId, message);

        // Build context
        String context = includeContext ? chatMemoryService.buildContextForAI(sessionId, historyLimit) : "";

        // Create system prompt with context
        String systemPrompt = "You are an AI assistant for a Virtualization Platform. " +
                "You help users manage mock services, API rules, and test scenarios. " +
                "Be helpful, concise, and technical.";

        String userPrompt = context + "\nUser: " + message;

        try {
            String response = aiService.generateCompletion(systemPrompt, userPrompt);

            // Save assistant response
            chatMemoryService.addAssistantMessage(sessionId, response);

            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("sessionId", sessionId);
            result.put("messageCount", chatMemoryService.getSessionMessageCount(sessionId));
            return result;
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "AI request failed: " + e.getMessage());
            error.put("sessionId", sessionId);
            return error;
        }
    }

    @GetMapping("/history/{sessionId}")
    public List<String> getHistory(@PathVariable String sessionId,
                                    @RequestParam(defaultValue = "20") int limit) {
        return chatMemoryService.getConversationHistory(sessionId, limit);
    }

    @DeleteMapping("/history/{sessionId}")
    public Map<String, Object> clearHistory(@PathVariable String sessionId) {
        chatMemoryService.clearSession(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "cleared");
        result.put("sessionId", sessionId);
        return result;
    }

    @GetMapping("/sessions")
    public Map<String, Object> getSessionInfo(@RequestParam(defaultValue = "default") String sessionId) {
        Map<String, Object> info = new HashMap<>();
        info.put("sessionId", sessionId);
        info.put("messageCount", chatMemoryService.getSessionMessageCount(sessionId));
        return info;
    }
}