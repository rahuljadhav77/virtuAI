package com.virtualization.controller;

import com.virtualization.dto.ApiResponse;
import com.virtualization.service.DocumentationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/docs")
public class DocumentationController {

    private final DocumentationService documentationService;

    public DocumentationController(DocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<Map<String, String>> generateDocumentation(@PathVariable Long serviceId) {
        String docs = documentationService.generateDocumentation(serviceId);
        return ApiResponse.success(Map.of(
                "markdown", docs,
                "serviceId", serviceId.toString()
        ), "Documentation generated successfully");
    }

    @GetMapping("/{serviceId}/quickstart")
    public ApiResponse<Map<String, String>> generateQuickStart(@PathVariable Long serviceId) {
        String quickStart = documentationService.generateQuickStart(serviceId);
        return ApiResponse.success(Map.of(
                "markdown", quickStart,
                "serviceId", serviceId.toString()
        ), "Quick start guide generated");
    }

    @GetMapping("/{serviceId}/reference")
    public ApiResponse<Map<String, String>> generateApiReference(@PathVariable Long serviceId) {
        String reference = documentationService.generateApiReference(serviceId);
        return ApiResponse.success(Map.of(
                "markdown", reference,
                "serviceId", serviceId.toString()
        ), "API reference generated");
    }

    @PostMapping("/{serviceId}/export")
    public ApiResponse<Map<String, String>> exportAsHtml(@PathVariable Long serviceId) {
        String docs = documentationService.generateDocumentation(serviceId);
        String html = convertMarkdownToHtml(docs);
        return ApiResponse.success(Map.of(
                "html", html,
                "serviceId", serviceId.toString()
        ), "Documentation exported as HTML");
    }

    private String convertMarkdownToHtml(String markdown) {
        return markdown
                .replaceAll("(?m)^# (.+)$", "<h1>$1</h1>")
                .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                .replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                .replaceAll("(?m)^\\*\\*(.+)\\*\\*$", "<strong>$1</strong>")
                .replaceAll("(?m)^\\*(.+)\\*$", "<em>$1</em>")
                .replaceAll("(?m)^- (.+)$", "<li>$1</li>")
                .replaceAll("(?m)^```json\\n([\\s\\S]*?)\\n```", "<pre><code class=\"json\">$1</code></pre>")
                .replaceAll("(?m)^```\\n?([\\s\\S]*?)\\n?```", "<pre><code>$1</code></pre>")
                .replaceAll("(?m)^(\\d+)\\. (.+)$", "<li>$2</li>")
                .replaceAll("\n\n", "</p><p>")
                .replaceAll("^", "<p>")
                .replaceAll("$", "</p>");
    }
}
