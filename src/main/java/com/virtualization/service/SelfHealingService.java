package com.virtualization.service;

import com.virtualization.dto.ApiResponse;
import com.virtualization.entity.TrafficLogEntity;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.repository.TrafficLogRepository;
import com.virtualization.repository.VirtualRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class SelfHealingService {

    private final AiService aiService;
    private final TrafficLogRepository trafficLogRepository;
    private final VirtualRuleRepository virtualRuleRepository;

    public SelfHealingService(AiService aiService,
                              TrafficLogRepository trafficLogRepository,
                              VirtualRuleRepository virtualRuleRepository) {
        this.aiService = aiService;
        this.trafficLogRepository = trafficLogRepository;
        this.virtualRuleRepository = virtualRuleRepository;
    }

    public String analyzeMismatch(Long serviceId, Long trafficLogId) {
        Optional<TrafficLogEntity> logOpt = trafficLogRepository.findById(trafficLogId);
        if (logOpt.isEmpty()) {
            throw new IllegalArgumentException("Traffic log not found: " + trafficLogId);
        }

        TrafficLogEntity trafficLog = logOpt.get();
        List<VirtualRuleEntity> rules = virtualRuleRepository.findByServiceId(serviceId);

        String systemPrompt = """
            You are a mock service expert analyzing differences between real API responses and virtual mock responses.
            Your goal is to identify discrepancies and suggest corrections to make mocks more accurate.

            Analyze the provided real traffic and current mock rules, then:
            1. Identify what fields/values differ between real response and mock
            2. Explain why the mock is incorrect
            3. Suggest specific corrections to make the mock match real behavior
            4. Provide the corrected mock rule JSON if possible

            Return your analysis in a structured format with sections:
            - **Differences Found**: List specific differences
            - **Analysis**: Why these differences matter
            - **Recommendations**: Specific corrections needed
            - **Corrected Rule JSON** (if applicable): Valid JSON for the corrected mock rule
            """;

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("## Real Traffic (ID: ").append(trafficLog.getId()).append(")\n");
        userPrompt.append("Method: ").append(trafficLog.getMethod()).append("\n");
        userPrompt.append("Path: ").append(trafficLog.getPath()).append("\n");
        userPrompt.append("Real Response Body:\n```json\n").append(trafficLog.getResponseBody()).append("\n```\n");
        userPrompt.append("Real Status Code: ").append(trafficLog.getStatusCode()).append("\n\n");

        userPrompt.append("## Current Mock Rules for Service ").append(serviceId).append("\n");
        if (rules.isEmpty()) {
            userPrompt.append("No rules defined for this service.\n");
        } else {
            for (VirtualRuleEntity rule : rules) {
                userPrompt.append("---\n");
                userPrompt.append("Rule ID: ").append(rule.getId()).append("\n");
                userPrompt.append("Name: ").append(rule.getName()).append("\n");
                userPrompt.append("Path Pattern: ").append(rule.getPathPattern()).append("\n");
                userPrompt.append("Method: ").append(rule.getMethod()).append("\n");
                userPrompt.append("Mock Response:\n```json\n").append(rule.getResponseBody()).append("\n```\n");
                userPrompt.append("Status Code: ").append(rule.getStatusCode()).append("\n");
                if (rule.getResponseHeaders() != null && !rule.getResponseHeaders().isEmpty()) {
                    userPrompt.append("Headers: ").append(rule.getResponseHeaders()).append("\n");
                }
            }
        }

        return aiService.generateCompletion(systemPrompt.toString(), userPrompt.toString());
    }

    public Map<String, Object> autoHealRule(Long ruleId, Long trafficLogId) {
        Optional<VirtualRuleEntity> ruleOpt = virtualRuleRepository.findById(ruleId);
        Optional<TrafficLogEntity> logOpt = trafficLogRepository.findById(trafficLogId);

        if (ruleOpt.isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }
        if (logOpt.isEmpty()) {
            throw new IllegalArgumentException("Traffic log not found: " + trafficLogId);
        }

        VirtualRuleEntity rule = ruleOpt.get();
        TrafficLogEntity trafficLog = logOpt.get();

        String systemPrompt = """
            You are a mock service expert. Given the current mock rule and a real API response,
            generate the corrected mock rule JSON.

            Return ONLY valid JSON with these fields:
            - responseBody: the corrected response JSON
            - statusCode: the correct HTTP status code
            - responseHeaders: object with any header corrections

            Do not include any explanation, only the JSON.
            """;

        String userPrompt = String.format("""
            Current Rule:
            - Path Pattern: %s
            - Method: %s
            - Current Response Body: %s
            - Current Status Code: %s

            Real Response Body: %s
            Real Status Code: %s
            """,
                rule.getPathPattern(),
                rule.getMethod(),
                rule.getResponseBody(),
                rule.getStatusCode(),
                trafficLog.getResponseBody(),
                trafficLog.getStatusCode()
        );

        String correctedJson = aiService.generateCompletion(systemPrompt, userPrompt);

        Map<String, Object> result = new HashMap<>();
        result.put("ruleId", ruleId);
        result.put("trafficLogId", trafficLogId);
        result.put("correctedJson", correctedJson);
        result.put("analysis", analyzeMismatch(rule.getServiceId(), trafficLogId));

        return result;
    }

    public List<Map<String, Object>> findDiscrepancies(Long serviceId, int limit) {
        List<TrafficLogEntity> recentLogs = trafficLogRepository.findTop50ByServiceIdOrderByTimestampDesc(serviceId);
        List<VirtualRuleEntity> rules = virtualRuleRepository.findByServiceId(serviceId);

        List<Map<String, Object>> discrepancies = new ArrayList<>();

        for (TrafficLogEntity log : recentLogs) {
            if (discrepancies.size() >= limit) break;

            VirtualRuleEntity matchingRule = findMatchingRule(log, rules);
            if (matchingRule == null) continue;

            String diff = compareResponses(log.getResponseBody(), matchingRule.getResponseBody());
            if (diff != null) {
                Map<String, Object> discrepancy = new HashMap<>();
                discrepancy.put("trafficLogId", log.getId());
                discrepancy.put("ruleId", matchingRule.getId());
                discrepancy.put("path", log.getPath());
                discrepancy.put("method", log.getMethod());
                discrepancy.put("differences", diff);
                discrepancies.add(discrepancy);
            }
        }

        return discrepancies;
    }

    private String compareResponses(String real, String mock) {
        if (real == null || mock == null) return null;

        boolean realIsJson = isValidJson(real);
        boolean mockIsJson = isValidJson(mock);

        if (!realIsJson || !mockIsJson) {
            if (!real.equals(mock)) {
                return "Non-JSON response differs:\nReal: " + real + "\nMock: " + mock;
            }
            return null;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode realNode =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(real);
            com.fasterxml.jackson.databind.JsonNode mockNode =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(mock);

            return findJsonDifferences(realNode, mockNode, "");
        } catch (Exception e) {
            log.warn("Failed to parse JSON for comparison", e);
            return null;
        }
    }

    private String findJsonDifferences(com.fasterxml.jackson.databind.JsonNode real,
                                       com.fasterxml.jackson.databind.JsonNode mock,
                                       String path) {
        StringBuilder diffs = new StringBuilder();

        if (real.getNodeType() != mock.getNodeType()) {
            diffs.append(path).append(": type mismatch - real is ")
                 .append(real.getNodeType()).append(", mock is ")
                 .append(mock.getNodeType()).append("\n");
            return diffs.toString();
        }

        if (real.isObject()) {
            Set<String> realKeys = new java.util.HashSet<>();
            real.fieldNames().forEachRemaining(realKeys::add);

            Set<String> mockKeys = new java.util.HashSet<>();
            mock.fieldNames().forEachRemaining(mockKeys::add);

            for (String key : realKeys) {
                String childPath = path.isEmpty() ? key : path + "." + key;
                if (!mockKeys.contains(key)) {
                    diffs.append(childPath).append(": missing in mock\n");
                } else {
                    String childDiff = findJsonDifferences(real.get(key), mock.get(key), childPath);
                    if (childDiff != null && !childDiff.isEmpty()) {
                        diffs.append(childDiff);
                    }
                }
            }

            for (String key : mockKeys) {
                if (!realKeys.contains(key)) {
                    String childPath = path.isEmpty() ? key : path + "." + key;
                    diffs.append(childPath).append(": present in mock but not in real\n");
                }
            }
        } else if (real.isArray()) {
            if (real.size() != mock.size()) {
                diffs.append(path).append(": array length differs - real has ")
                     .append(real.size()).append(", mock has ").append(mock.size()).append("\n");
            } else {
                for (int i = 0; i < Math.min(real.size(), mock.size()); i++) {
                    String childDiff = findJsonDifferences(real.get(i), mock.get(i), path + "[" + i + "]");
                    if (childDiff != null && !childDiff.isEmpty()) {
                        diffs.append(childDiff);
                    }
                }
            }
        } else {
            if (!real.equals(mock)) {
                diffs.append(path).append(": value differs - real is \"").append(real.asText())
                     .append("\", mock is \"").append(mock.asText()).append("\"\n");
            }
        }

        return diffs.toString();
    }

    private boolean isValidJson(String text) {
        try {
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private VirtualRuleEntity findMatchingRule(TrafficLogEntity log, List<VirtualRuleEntity> rules) {
        for (VirtualRuleEntity rule : rules) {
            boolean methodMatch = rule.getMethod() == null ||
                                 rule.getMethod().equalsIgnoreCase(log.getMethod());
            boolean pathMatch = rule.getPathPattern() == null ||
                                log.getPath().matches(rule.getPathPattern().replace("*", ".*"));
            if (methodMatch && pathMatch) {
                return rule;
            }
        }
        return null;
    }

    public String getHealthReport(Long serviceId) {
        List<Map<String, Object>> discrepancies = findDiscrepancies(serviceId, 10);
        int totalLogs = trafficLogRepository.findTop50ByServiceIdOrderByTimestampDesc(serviceId).size();

        String systemPrompt = "You are a mock health analyst. Generate a brief health report.";
        String userPrompt = String.format("""
            Service ID: %d
            Total recent requests: %d
            Discrepancies found: %d

            %s

            Provide a brief health summary and top 3 recommendations.
            """,
            serviceId,
            totalLogs,
            discrepancies.size(),
            discrepancies.isEmpty() ? "No discrepancies found." :
                "Discrepancies:\n" + discrepancies.stream()
                    .map(d -> "- " + d.get("path") + ": " + d.get("differences"))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("")
        );

        return aiService.generateCompletion(systemPrompt, userPrompt);
    }
}