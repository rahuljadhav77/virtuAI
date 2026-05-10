package com.virtualization.service;

import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.entity.VirtualServiceEntity;
import com.virtualization.exception.ImportValidationException;
import com.virtualization.exception.OpenApiParseException;
import com.virtualization.repository.VirtualRuleRepository;
import com.virtualization.repository.VirtualServiceRepository;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ImporterService {
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{[^}]+\\}");

    private final VirtualServiceRepository serviceRepository;
    private final VirtualRuleRepository ruleRepository;

    public ImporterService(VirtualServiceRepository serviceRepository, VirtualRuleRepository ruleRepository) {
        this.serviceRepository = serviceRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional
    public VirtualServiceEntity importOpenApi(String spec, String serviceName, String type) {
        if (spec == null || spec.isBlank()) {
            throw new ImportValidationException("OpenAPI spec cannot be null or empty");
        }

        log.info("Starting OpenAPI import, serviceName={}", serviceName);

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec, null, new ParseOptions());
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            String messages = result.getMessages() != null ? String.join(", ", result.getMessages()) : "Unknown parse error";
            log.error("Failed to parse OpenAPI spec: {}", messages);
            throw new OpenApiParseException("Failed to parse OpenAPI spec: " + messages);
        }

        if (openAPI.getInfo() == null) {
            throw new OpenApiParseException("OpenAPI spec is missing Info object");
        }

        VirtualServiceEntity vService = new VirtualServiceEntity();
        vService.setName(serviceName != null && !serviceName.isEmpty() ? serviceName : openAPI.getInfo().getTitle());
        vService.setType(type != null ? type : "REST");
        vService.setEnabled(true);
        vService = serviceRepository.save(vService);

        Long serviceId = vService.getId();
        List<VirtualRuleEntity> rules = new ArrayList<>();

        openAPI.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                VirtualRuleEntity rule = createRule(serviceId, path, method.name(), operation);
                rules.add(rule);
            });
        });

        if (!rules.isEmpty()) {
            ruleRepository.saveAll(rules);
        }

        log.info("OpenAPI import completed: serviceId={}, rulesCreated={}", serviceId, rules.size());
        return vService;
    }

    public List<VirtualRuleEntity> previewOpenApi(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new ImportValidationException("OpenAPI spec cannot be null or empty");
        }

        log.info("Generating OpenAPI preview");

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec, null, new ParseOptions());
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            String messages = result.getMessages() != null ? String.join(", ", result.getMessages()) : "Unknown parse error";
            log.error("Failed to parse OpenAPI spec: {}", messages);
            throw new OpenApiParseException("Failed to parse OpenAPI spec: " + messages);
        }

        List<VirtualRuleEntity> rules = new ArrayList<>();
        openAPI.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                VirtualRuleEntity rule = createRule(null, path, method.name(), operation);
                rules.add(rule);
            });
        });

        log.info("Preview generated: {} rules", rules.size());
        return rules;
    }

    @Transactional
    public VirtualServiceEntity importJsonRules(List<VirtualRuleEntity> rules, String serviceName, String type) {
        if (rules == null || rules.isEmpty()) {
            throw new ImportValidationException("Rules list cannot be null or empty");
        }

        log.info("Starting JSON rules import, serviceName={}, ruleCount={}", serviceName, rules.size());

        VirtualServiceEntity vService = new VirtualServiceEntity();
        vService.setName(serviceName);
        vService.setType(type != null ? type : "BULK_JSON");
        vService.setEnabled(true);
        vService = serviceRepository.save(vService);

        Long serviceId = vService.getId();
        rules.forEach(rule -> rule.setServiceId(serviceId));
        ruleRepository.saveAll(rules);

        log.info("JSON rules import completed: serviceId={}, rulesCreated={}", serviceId, rules.size());
        return vService;
    }

    private VirtualRuleEntity createRule(Long serviceId, String path, String method, Operation operation) {
        VirtualRuleEntity rule = new VirtualRuleEntity();
        rule.setServiceId(serviceId);

        String name = operation.getOperationId();
        if (name == null || name.isBlank()) {
            name = operation.getSummary();
        }
        if (name == null || name.isBlank()) {
            name = method + " " + path;
        }
        rule.setName(name);

        rule.setMethod(method);

        String pathPattern = "^" + PATH_PARAM_PATTERN.matcher(path).replaceAll("[^/]+") + "$";
        rule.setPathPattern(pathPattern);

        rule.setStatusCode(getStatusCode(operation));

        String responseBody = operation.getOperationId() != null
                ? "{\"message\": \"Mock response for " + operation.getOperationId() + "\"}"
                : "{\"message\": \"Mock response for " + path + "\"}";
        rule.setResponseBody(responseBody);

        rule.setPriority(1);

        if (operation.getRequestBody() != null) {
            rule.setRequestSchema(operation.getRequestBody().getContent().values().stream()
                    .findFirst()
                    .map(c -> c.getSchema().toString())
                    .orElse(null));
        }

        return rule;
    }

    private int getStatusCode(Operation operation) {
        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            return 200;
        }

        for (Map.Entry<String, ?> responseEntry : operation.getResponses().entrySet()) {
            try {
                int code = Integer.parseInt(responseEntry.getKey());
                if (code >= 200 && code < 300) {
                    return code;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String firstKey = operation.getResponses().keySet().stream().findFirst().orElse("200");
        try {
            return Integer.parseInt(firstKey);
        } catch (NumberFormatException e) {
            return 200;
        }
    }
}
