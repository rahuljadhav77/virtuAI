package com.virtualization.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.entity.VirtualServiceEntity;
import com.virtualization.service.ImporterService;
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
    public VirtualServiceEntity importOpenApi(@RequestBody Map<String, String> request) {
        String spec = request.get("spec");
        String name = request.get("name");
        String type = request.get("type");
        return importerService.importOpenApi(spec, name, type);
    }

    @PostMapping("/json")
    public VirtualServiceEntity importJson(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String type = (String) request.get("type");
        List<VirtualRuleEntity> rules = objectMapper.convertValue(request.get("rules"), new TypeReference<List<VirtualRuleEntity>>() {});
        return importerService.importJsonRules(rules, name, type);
    }
}
