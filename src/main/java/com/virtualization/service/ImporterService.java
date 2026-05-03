package com.virtualization.service;

import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.entity.VirtualServiceEntity;
import com.virtualization.repository.VirtualRuleRepository;
import com.virtualization.repository.VirtualServiceRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ImporterService {
    private final VirtualServiceRepository serviceRepository;
    private final VirtualRuleRepository ruleRepository;

    public ImporterService(VirtualServiceRepository serviceRepository, VirtualRuleRepository ruleRepository) {
        this.serviceRepository = serviceRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional
    public VirtualServiceEntity importOpenApi(String spec, String serviceName) {
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec, null, new ParseOptions());
        OpenAPI openAPI = result.getOpenAPI();
        
        if (openAPI == null) {
            throw new RuntimeException("Failed to parse OpenAPI spec: " + result.getMessages());
        }

        VirtualServiceEntity vService = new VirtualServiceEntity();
        vService.setName(serviceName != null && !serviceName.isEmpty() ? serviceName : openAPI.getInfo().getTitle());
        vService.setType("REST");
        vService.setEnabled(true);
        vService = serviceRepository.save(vService);

        final Long serviceId = vService.getId();
        openAPI.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                VirtualRuleEntity rule = new VirtualRuleEntity();
                rule.setServiceId(serviceId);
                rule.setName(operation.getOperationId() != null ? operation.getOperationId() : method + " " + path);
                rule.setMethod(method.name());
                // Simple conversion from {id} to .*
                rule.setPathPattern("^" + path.replaceAll("\\{.*?\\}", "[^/]+") + "$");
                rule.setStatusCode(200);
                rule.setResponseBody("{\"message\": \"Mock response for " + (operation.getOperationId() != null ? operation.getOperationId() : path) + "\"}");
                rule.setPriority(1);
                ruleRepository.save(rule);
            });
        });

        return vService;
    }

    @Transactional
    public VirtualServiceEntity importJsonRules(List<VirtualRuleEntity> rules, String serviceName) {
        VirtualServiceEntity vService = new VirtualServiceEntity();
        vService.setName(serviceName);
        vService.setType("BULK_JSON");
        vService.setEnabled(true);
        vService = serviceRepository.save(vService);
        
        final Long serviceId = vService.getId();
        for (VirtualRuleEntity rule : rules) {
            rule.setServiceId(serviceId);
            ruleRepository.save(rule);
        }
        return vService;
    }
}
