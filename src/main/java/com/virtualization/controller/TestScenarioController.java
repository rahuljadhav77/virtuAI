package com.virtualization.controller;

import com.virtualization.dto.ApiResponse;
import com.virtualization.dto.CountResult;
import com.virtualization.service.TestScenarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/test-scenarios")
public class TestScenarioController {

    private final TestScenarioService testScenarioService;

    public TestScenarioController(TestScenarioService testScenarioService) {
        this.testScenarioService = testScenarioService;
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<Map<String, Object>> generateTestScenarios(
            @PathVariable Long serviceId,
            @RequestParam(required = false, defaultValue = "all") String type) {
        String scenarios = testScenarioService.generateTestScenarios(serviceId, type);
        List<Map<String, Object>> parsed = testScenarioService.parseTestScenarios(scenarios);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("scenarios", scenarios);
        result.put("parsed", parsed);
        result.put("serviceId", serviceId);
        return ApiResponse.success(result, "Test scenarios generated", parsed.size());
    }

    @PostMapping("/openapi")
    public ApiResponse<Map<String, Object>> generateFromOpenApi(
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false, defaultValue = "all") String type) {
        String spec = (String) request.get("spec");
        if (spec == null || spec.isBlank()) {
            return ApiResponse.error("'spec' field is required");
        }

        String scenarios = testScenarioService.generateOpenApiTestScenarios(spec, type);
        List<Map<String, Object>> parsed = testScenarioService.parseTestScenarios(scenarios);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("scenarios", scenarios);
        result.put("parsed", parsed);
        return ApiResponse.success(result, "Test scenarios generated from OpenAPI spec", parsed.size());
    }

    @GetMapping("/{serviceId}/count")
    public ApiResponse<Map<String, Object>> getScenarioCount(
            @PathVariable Long serviceId,
            @RequestParam(required = false, defaultValue = "all") String type) {
        String scenarios = testScenarioService.generateTestScenarios(serviceId, type);
        List<Map<String, Object>> parsed = testScenarioService.parseTestScenarios(scenarios);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("count", parsed.size());
        result.put("serviceId", serviceId);
        return ApiResponse.success(result, "Scenario count retrieved");
    }
}
