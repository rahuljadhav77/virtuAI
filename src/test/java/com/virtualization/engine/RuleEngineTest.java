package com.virtualization.engine;

import com.samskivert.mustache.Mustache;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.model.VirtualRequest;
import com.virtualization.service.CachedRuleService;
import com.virtualization.service.SchemaService;
import com.virtualization.service.ScriptingService;
import com.virtualization.service.StateService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleEngineTest {

    private CachedRuleService cachedRuleService;
    private StateService stateService;
    private SchemaService schemaService;
    private ScriptingService scriptingService;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setup() {
        cachedRuleService = mock(CachedRuleService.class);
        stateService = mock(StateService.class);
        schemaService = mock(SchemaService.class);
        scriptingService = mock(ScriptingService.class);

        ruleEngine = new RuleEngine(
                cachedRuleService,
                Mustache.compiler(),
                stateService,
                new SimpleMeterRegistry(),
                scriptingService,
                schemaService
        );
    }

    @Test
    void shouldNotMatchSchemaRuleWhenBodyIsMissing() {
        VirtualRuleEntity rule = new VirtualRuleEntity();
        rule.setName("Validated Rule");
        rule.setPathPattern("/validated");
        rule.setMethod("POST");
        rule.setRequestSchema("{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}, \"required\": [\"id\"]}");
        rule.setResponseBody("{\"status\": \"Valid JSON received\"}");
        rule.setStatusCode(200);

        when(cachedRuleService.getRulesByMethod("POST")).thenReturn(List.of(rule));

        VirtualRequest request = VirtualRequest.builder()
                .path("/validated")
                .method("POST")
                .headers(Map.of())
                .body(null)
                .queryParams(Map.of())
                .build();

        assertTrue(ruleEngine.evaluate(request).isEmpty());
    }

    @Test
    void shouldNotMatchJsonPathRuleWhenBodyIsMissing() {
        VirtualRuleEntity rule = new VirtualRuleEntity();
        rule.setName("JsonPath Rule");
        rule.setPathPattern("/jsonpath");
        rule.setMethod("POST");
        rule.setJsonPathCondition("$.id");
        rule.setJsonPathValue("123");
        rule.setResponseBody("{\"status\": \"missing body\"}");
        rule.setStatusCode(200);

        when(cachedRuleService.getRulesByMethod("POST")).thenReturn(List.of(rule));

        VirtualRequest request = VirtualRequest.builder()
                .path("/jsonpath")
                .method("POST")
                .headers(Map.of())
                .body(null)
                .queryParams(Map.of())
                .build();

        assertTrue(ruleEngine.evaluate(request).isEmpty());
    }
}
