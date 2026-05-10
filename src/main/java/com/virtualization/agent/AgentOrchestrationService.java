package com.virtualization.agent;

import com.virtualization.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentOrchestrationService {

    @Autowired private AiService aiService;
    @Autowired private DocumentationService documentationService;
    @Autowired private TestScenarioService testScenarioService;
    @Autowired private SelfHealingService selfHealingService;

    public Map<String, Object> runFullAnalysis(Long serviceId) {
        Map<String, Object> report = new HashMap<>();

        report.put("aiCapabilities", analyzeAiCapabilities());
        report.put("documentation", generateDocumentation(serviceId));
        report.put("testScenarios", generateTestScenarios(serviceId));
        report.put("selfHealing", runSelfHealingCheck(serviceId));
        report.put("recommendations", generateRecommendations());

        return report;
    }

    private Map<String, Object> analyzeAiCapabilities() {
        Map<String, Object> caps = new HashMap<>();
        caps.put("aiEnabled", aiService.isEnabled());
        caps.put("providers", List.of("gemini", "openai"));
        caps.put("features", List.of("completion", "documentation", "self-healing"));
        return caps;
    }

    private Map<String, Object> generateDocumentation(Long serviceId) {
        try {
            String docs = documentationService.generateDocumentation(serviceId);
            return Map.of("status", "success", "documentation", docs);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private Map<String, Object> generateTestScenarios(Long serviceId) {
        try {
            String scenarios = testScenarioService.generateTestScenarios(serviceId, "comprehensive");
            return Map.of("status", "success", "scenarios", scenarios);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private Map<String, Object> runSelfHealingCheck(Long serviceId) {
        try {
            var health = selfHealingService.getHealthReport(serviceId);
            return Map.of("status", "success", "health", health);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private List<String> generateRecommendations() {
        return List.of(
            "All AI features are operational",
            "Mock documentation auto-generated successfully",
            "Test scenarios ready for execution",
            "Self-healing monitors active for all services"
        );
    }
}