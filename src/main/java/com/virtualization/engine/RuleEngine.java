package com.virtualization.engine;

import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.model.VirtualRequest;
import com.virtualization.model.VirtualResponse;
import com.virtualization.service.CachedRuleService;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import com.virtualization.service.SchemaService;
import com.virtualization.service.ScriptingService;
import com.virtualization.service.StateService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import com.jayway.jsonpath.JsonPath;

@Service
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);
    private final CachedRuleService cachedRuleService;
    private final Mustache.Compiler mustacheCompiler;
    private final StateService stateService;
    private final MeterRegistry meterRegistry;
    private final ScriptingService scriptingService;
    private final SchemaService schemaService;

    public RuleEngine(CachedRuleService cachedRuleService, 
                      Mustache.Compiler mustacheCompiler, 
                      StateService stateService, 
                      MeterRegistry meterRegistry,
                      ScriptingService scriptingService,
                      SchemaService schemaService) {
        this.cachedRuleService = cachedRuleService;
        this.mustacheCompiler = mustacheCompiler;
        this.stateService = stateService;
        this.meterRegistry = meterRegistry;
        this.scriptingService = scriptingService;
        this.schemaService = schemaService;
    }

    public Optional<VirtualResponse> evaluate(VirtualRequest request) {
        String contextId = request.getHeaders().getOrDefault("x-context-id", "default");
        List<VirtualRuleEntity> rules = cachedRuleService.getRulesByMethod(request.getMethod());

        return rules.stream()
                .sorted(Comparator.comparingInt(VirtualRuleEntity::getPriority).reversed())
                .filter(rule -> matches(rule, request, contextId))
                .map(rule -> {
                    if (rule.getNewState() != null) {
                        log.info("Rule {} setting new state: {}", rule.getName(), rule.getNewState());
                        stateService.setState(contextId, "status", rule.getNewState());
                    }
                    meterRegistry.counter("virtualization.http.rule.hit", "rule", rule.getName()).increment();
                    return buildResponse(rule, request);
                })
                .findFirst();
    }

    private boolean matches(VirtualRuleEntity rule, VirtualRequest request, String contextId) {
        log.debug("Checking rule: {} against request: {} {}", rule.getName(), request.getMethod(), request.getPath());

        // 1. Stateful Check
        if (rule.getRequiredState() != null) {
            String currentState = stateService.getState(contextId, "status");
            if (!rule.getRequiredState().equals(currentState)) {
                return false;
            }
        }
        
        // 2. Path Regex Match
        if (rule.getPathPattern() != null) {
            try {
                if (!Pattern.matches(rule.getPathPattern(), request.getPath())) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        // 3. Schema Validation
        if (rule.getRequestSchema() != null) {
            if (request.getBody() == null || !schemaService.validate(request.getBody(), rule.getRequestSchema())) {
                log.debug("Rule {} schema validation failed", rule.getName());
                return false;
            }
        }

        // 4. JSONPath Body Match
        if (rule.getJsonPathCondition() != null) {
            if (request.getBody() == null) {
                return false;
            }
            try {
                Object value = JsonPath.read(request.getBody(), rule.getJsonPathCondition());
                if (rule.getJsonPathValue() != null && !rule.getJsonPathValue().equals(value.toString())) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    private VirtualResponse buildResponse(VirtualRuleEntity rule, VirtualRequest request) {
        Template template = mustacheCompiler.compile(rule.getResponseBody());
        Map<String, Object> context = new HashMap<>();
        context.put("request", request);
        
        String body = template.execute(context);

        if (rule.getScript() != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("request", request);
            vars.put("body", body);
            Object result = scriptingService.execute(rule.getScript(), vars);
            if (result != null) {
                body = result.toString();
            }
        }

        return VirtualResponse.builder()
                .statusCode(rule.getStatusCode())
                .body(body)
                .headers(rule.getResponseHeaders())
                .delayMs(rule.getDelayMs())
                .build();
    }
}
