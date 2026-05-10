package com.virtualization.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualization.dto.ApiResponse;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.entity.VirtualServiceEntity;
import com.virtualization.service.ImporterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/import")
public class ImporterController {

    private final ImporterService importerService;
    private final ObjectMapper objectMapper;

    public ImporterController(ImporterService importerService, ObjectMapper objectMapper) {
        this.importerService = importerService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/openapi")
    public ApiResponse<VirtualServiceEntity> importOpenApi(@RequestBody Map<String, String> request) {
        String spec = request.get("spec");
        String name = request.get("name");
        String type = request.get("type");

        if (spec == null || spec.isBlank()) {
            return ApiResponse.error("'spec' field is required and cannot be empty");
        }

        VirtualServiceEntity service = importerService.importOpenApi(spec, name, type);
        return ApiResponse.success(service, "OpenAPI spec imported successfully", 1);
    }

    @PostMapping("/openapi/preview")
    public ApiResponse<List<VirtualRuleEntity>> previewOpenApi(@RequestBody Map<String, String> request) {
        String spec = request.get("spec");

        if (spec == null || spec.isBlank()) {
            return ApiResponse.error("'spec' field is required and cannot be empty");
        }

        List<VirtualRuleEntity> rules = importerService.previewOpenApi(spec);
        return ApiResponse.success(rules, "Preview generated", rules.size());
    }

    @PostMapping("/json")
    public ApiResponse<VirtualServiceEntity> importJson(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String type = (String) request.get("type");

        if (name == null || name.isBlank()) {
            return ApiResponse.error("'name' field is required and cannot be empty");
        }

        Object rulesObj = request.get("rules");
        if (rulesObj == null) {
            return ApiResponse.error("'rules' field is required");
        }

        List<VirtualRuleEntity> rules = objectMapper.convertValue(rulesObj, new TypeReference<List<VirtualRuleEntity>>() {});
        VirtualServiceEntity service = importerService.importJsonRules(rules, name, type);
        return ApiResponse.success(service, "JSON rules imported successfully", rules.size());
    }
}
