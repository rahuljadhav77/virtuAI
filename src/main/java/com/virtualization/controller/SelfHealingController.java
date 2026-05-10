package com.virtualization.controller;

import com.virtualization.dto.ApiResponse;
import com.virtualization.service.SelfHealingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/self-healing")
public class SelfHealingController {

    private final SelfHealingService selfHealingService;

    public SelfHealingController(SelfHealingService selfHealingService) {
        this.selfHealingService = selfHealingService;
    }

    @GetMapping("/health/{serviceId}")
    public ApiResponse<Map<String, Object>> getHealthReport(@PathVariable Long serviceId) {
        String report = selfHealingService.getHealthReport(serviceId);
        return ApiResponse.success(Map.of("serviceId", serviceId, "report", report), "Health report generated");
    }

    @GetMapping("/analyze/{serviceId}/{trafficLogId}")
    public ApiResponse<Map<String, Object>> analyzeMismatch(
            @PathVariable Long serviceId,
            @PathVariable Long trafficLogId) {
        String analysis = selfHealingService.analyzeMismatch(serviceId, trafficLogId);
        return ApiResponse.success(Map.of(
                "serviceId", serviceId,
                "trafficLogId", trafficLogId,
                "analysis", analysis
        ), "Mismatch analysis complete");
    }

    @PostMapping("/auto-heal")
    public ApiResponse<Map<String, Object>> autoHealRule(
            @RequestParam Long ruleId,
            @RequestParam Long trafficLogId) {
        Map<String, Object> result = selfHealingService.autoHealRule(ruleId, trafficLogId);
        return ApiResponse.success(result, "Auto-heal suggestions generated");
    }

    @GetMapping("/discrepancies/{serviceId}")
    public ApiResponse<Map<String, Object>> findDiscrepancies(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> discrepancies = selfHealingService.findDiscrepancies(serviceId, limit);
        return ApiResponse.success(Map.of(
                "serviceId", serviceId,
                "count", discrepancies.size(),
                "discrepancies", discrepancies
        ), "Discrepancies found: " + discrepancies.size());
    }
}