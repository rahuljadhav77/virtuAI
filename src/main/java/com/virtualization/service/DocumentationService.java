package com.virtualization.service;

import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.entity.VirtualServiceEntity;
import com.virtualization.repository.VirtualRuleRepository;
import com.virtualization.repository.VirtualServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class DocumentationService {

    private static final String SYSTEM_PROMPT = """
            You are a technical documentation writer for API mock services.
            Generate clear, professional documentation in Markdown format.
            Include code examples in curl and JavaScript fetch syntax.
            """;

    private final AiService aiService;
    private final VirtualServiceRepository serviceRepository;
    private final VirtualRuleRepository ruleRepository;

    public DocumentationService(AiService aiService, VirtualServiceRepository serviceRepository, VirtualRuleRepository ruleRepository) {
        this.aiService = aiService;
        this.serviceRepository = serviceRepository;
        this.ruleRepository = ruleRepository;
    }

    public String generateDocumentation(Long serviceId) {
        VirtualServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        List<VirtualRuleEntity> rules = ruleRepository.findByServiceId(serviceId);

        StringBuilder context = new StringBuilder();
        context.append("# Service: ").append(service.getName()).append("\n\n");
        context.append("- Type: ").append(service.getType()).append("\n");
        context.append("- Status: ").append(service.isEnabled() ? "Active" : "Disabled").append("\n\n");

        context.append("## Endpoints\n\n");
        for (VirtualRuleEntity rule : rules) {
            context.append("### ").append(rule.getMethod()).append(" ").append(rule.getPathPattern()).append("\n\n");
            context.append("- Name: ").append(rule.getName()).append("\n");
            if (rule.getResponseBody() != null) {
                context.append("- Response: ```json\n").append(rule.getResponseBody()).append("\n```\n");
            }
            context.append("- Status Code: ").append(rule.getStatusCode()).append("\n\n");
        }

        String userPrompt = """
                Based on the following mock service configuration, generate comprehensive documentation:

                %s

                Please include:
                1. Service overview and purpose
                2. Authentication requirements (if any)
                3. Detailed endpoint documentation with example requests/responses
                4. Error handling information
                5. Usage examples in curl and JavaScript
                """.formatted(context);

        log.info("Generating documentation for service: {}", service.getName());
        return aiService.generateCompletion(SYSTEM_PROMPT, userPrompt);
    }

    public String generateQuickStart(Long serviceId) {
        VirtualServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        List<VirtualRuleEntity> rules = ruleRepository.findByServiceId(serviceId);

        StringBuilder context = new StringBuilder();
        context.append("Service: ").append(service.getName()).append("\n");
        context.append("Type: ").append(service.getType()).append("\n");
        context.append("Endpoints:\n");

        for (VirtualRuleEntity rule : rules) {
            context.append("- ").append(rule.getMethod()).append(" ").append(rule.getPathPattern());
            context.append(" -> ").append(rule.getStatusCode()).append("\n");
        }

        String userPrompt = """
                Generate a quick start guide for this mock API:

                %s

                Include:
                1. How to make your first request
                2. Base URL pattern
                3. Simple curl example
                4. Common first steps
                Keep it concise and beginner-friendly.
                """.formatted(context);

        log.info("Generating quick start for service: {}", service.getName());
        return aiService.generateCompletion(SYSTEM_PROMPT, userPrompt);
    }

    public String generateApiReference(Long serviceId) {
        VirtualServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        List<VirtualRuleEntity> rules = ruleRepository.findByServiceId(serviceId);

        StringBuilder context = new StringBuilder();
        for (VirtualRuleEntity rule : rules) {
            context.append("Endpoint:\n");
            context.append("  Method: ").append(rule.getMethod()).append("\n");
            context.append("  Path: ").append(rule.getPathPattern()).append("\n");
            context.append("  Name: ").append(rule.getName()).append("\n");
            if (rule.getResponseBody() != null) {
                context.append("  Response Body:\n").append(rule.getResponseBody()).append("\n");
            }
            if (rule.getRequestSchema() != null) {
                context.append("  Request Schema: ").append(rule.getRequestSchema()).append("\n");
            }
            context.append("\n");
        }

        String userPrompt = """
                Generate a detailed API reference for these endpoints:

                %s

                For each endpoint, document:
                1. Full request format
                2. Request headers and body
                3. Response format and status codes
                4. All possible error responses
                Use consistent formatting with tables.
                """.formatted(context);

        log.info("Generating API reference for service: {}", service.getName());
        return aiService.generateCompletion(SYSTEM_PROMPT, userPrompt);
    }
}
