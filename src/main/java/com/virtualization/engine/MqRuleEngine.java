package com.virtualization.engine;

import com.virtualization.entity.MqRuleEntity;
import com.virtualization.model.MqMessage;
import com.virtualization.repository.MqRuleRepository;
import com.virtualization.service.ScriptingService;
import com.virtualization.service.StateService;
import com.samskivert.mustache.Mustache;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.jayway.jsonpath.JsonPath;

import java.util.*;

@Service
public class MqRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(MqRuleEngine.class);
    private final MqRuleRepository repository;
    private final Mustache.Compiler mustacheCompiler;
    private final StateService stateService;
    private final MeterRegistry meterRegistry;
    private final ScriptingService scriptingService;

    public MqRuleEngine(MqRuleRepository repository, 
                        Mustache.Compiler mustacheCompiler, 
                        StateService stateService, 
                        MeterRegistry meterRegistry,
                        ScriptingService scriptingService) {
        this.repository = repository;
        this.mustacheCompiler = mustacheCompiler;
        this.stateService = stateService;
        this.meterRegistry = meterRegistry;
        this.scriptingService = scriptingService;
    }

    public Optional<MqMessage> process(String queueName, MqMessage request) {
        log.debug("Processing MQ message from queue: {}", queueName);
        String contextId = request.getCorrelationId() != null ? request.getCorrelationId() : "mq-default";
        List<MqRuleEntity> rules = repository.findByInputQueue(queueName);

        return rules.stream()
                .sorted(Comparator.comparingInt(MqRuleEntity::getPriority).reversed())
                .filter(rule -> matches(rule, request, contextId))
                .map(rule -> {
                    if (rule.getNewState() != null) {
                        stateService.setState(contextId, "status", rule.getNewState());
                    }
                    meterRegistry.counter("virtualization.mq.rule.hit", "rule", rule.getName()).increment();
                    return buildResponse(rule, request);
                })
                .findFirst();
    }

    private boolean matches(MqRuleEntity rule, MqMessage request, String contextId) {
        // Stateful Check
        if (rule.getRequiredState() != null) {
            String currentState = stateService.getState(contextId, "status");
            if (!rule.getRequiredState().equals(currentState)) {
                return false;
            }
        }
        if (rule.getJsonPathCondition() != null && request.getPayload() != null) {
            try {
                Object value = JsonPath.read(request.getPayload(), rule.getJsonPathCondition());
                if (rule.getJsonPathValue() != null && !rule.getJsonPathValue().equals(value.toString())) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private MqMessage buildResponse(MqRuleEntity rule, MqMessage request) {
        String payload = mustacheCompiler.compile(rule.getResponsePayload()).execute(Map.of("request", request));
        
        if (rule.getScript() != null) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("request", request);
            vars.put("payload", payload);
            Object result = scriptingService.execute(rule.getScript(), vars);
            if (result != null) {
                payload = result.toString();
            }
        }

        return MqMessage.builder()
                .messageId("MOCK-" + System.currentTimeMillis())
                .correlationId(request.getCorrelationId())
                .payload(payload)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
