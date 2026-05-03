package com.virtualization.config;

import com.virtualization.entity.MqRuleEntity;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.repository.MqRuleRepository;
import com.virtualization.repository.VirtualRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(VirtualRuleRepository repository, MqRuleRepository mqRepository) {
        return args -> {
            seedHttpRules(repository);
            seedMqRules(mqRepository);
        };
    }

    private void seedHttpRules(VirtualRuleRepository repository) {
        // Rule 1: Regex Match + Dynamic Response
        VirtualRuleEntity rule1 = new VirtualRuleEntity();
        rule1.setName("Greeting Rule");
        rule1.setPathPattern("/hello/.*");
        rule1.setMethod("GET");
        rule1.setResponseBody("{\"message\": \"Hello from virtualization platform!\", \"path\": \"{{request.path}}\"}");
        rule1.setStatusCode(200);
        rule1.setResponseHeaders(Map.of("Content-Type", "application/json"));
        repository.save(rule1);

        // Rule 2: JSONPath Match
        VirtualRuleEntity rule2 = new VirtualRuleEntity();
        rule2.setName("Premium Flow");
        rule2.setPathPattern("/payment");
        rule2.setMethod("POST");
        rule2.setJsonPathCondition("$.amount");
        rule2.setJsonPathValue("1000");
        rule2.setResponseBody("{\"status\": \"approved\", \"tier\": \"premium\"}");
        rule2.setStatusCode(201);
        rule2.setResponseHeaders(Map.of("Content-Type", "application/json"));
        repository.save(rule2);
        
        // Rule 3: Legacy match
        VirtualRuleEntity rule3 = new VirtualRuleEntity();
        rule3.setName("Legacy Customer");
        rule3.setPathPattern("/customer");
        rule3.setMethod("GET");
        rule3.setResponseBody("{\"id\": 1, \"name\": \"Legacy User\"}");
        rule3.setStatusCode(200);
        repository.save(rule3);

        // Rule 4: Priority Test - High
        VirtualRuleEntity pHigh = new VirtualRuleEntity();
        pHigh.setName("Priority High");
        pHigh.setPathPattern("/priority");
        pHigh.setMethod("GET");
        pHigh.setResponseBody("{\"result\": \"HIGH\"}");
        pHigh.setStatusCode(200);
        pHigh.setPriority(10);
        repository.save(pHigh);

        // Rule 5: Priority Test - Low
        VirtualRuleEntity pLow = new VirtualRuleEntity();
        pLow.setName("Priority Low");
        pLow.setPathPattern("/priority");
        pLow.setMethod("GET");
        pLow.setResponseBody("{\"result\": \"LOW\"}");
        pLow.setStatusCode(200);
        pLow.setPriority(1);
        repository.save(pLow);

        // Rule 6: Stateful Sequence - Step 1
        VirtualRuleEntity s1 = new VirtualRuleEntity();
        s1.setName("State Step 1");
        s1.setPathPattern("/sequence");
        s1.setMethod("GET");
        s1.setResponseBody("{\"step\": 1, \"next\": \"CALL_AGAIN\"}");
        s1.setStatusCode(200);
        s1.setNewState("STEP_2");
        s1.setPriority(5);
        repository.save(s1);

        // Rule 7: Stateful Sequence - Step 2
        VirtualRuleEntity s2 = new VirtualRuleEntity();
        s2.setName("State Step 2");
        s2.setPathPattern("/sequence");
        s2.setMethod("GET");
        s2.setResponseBody("{\"step\": 2, \"next\": \"FINISH\"}");
        s2.setStatusCode(200);
        s2.setRequiredState("STEP_2");
        s2.setNewState("FINISHED");
        s2.setPriority(10); // Higher priority so it's checked first if state matches
        repository.save(s2);

        // Rule 8: Dynamic Scripting Test
        VirtualRuleEntity scriptRule = new VirtualRuleEntity();
        scriptRule.setName("Scripted Rule");
        scriptRule.setPathPattern("/scripted");
        scriptRule.setMethod("GET");
        scriptRule.setResponseBody("Mustache body"); // Will be overridden by script
        scriptRule.setStatusCode(200);
        scriptRule.setScript("return 'Dynamic response from Groovy: ' + (50 * 2)");
        repository.save(scriptRule);

        // Rule 9: Schema Validation Test
        VirtualRuleEntity schemaRule = new VirtualRuleEntity();
        schemaRule.setName("Validated Rule");
        schemaRule.setPathPattern("/validated");
        schemaRule.setMethod("POST");
        schemaRule.setResponseBody("{\"status\": \"Valid JSON received\"}");
        schemaRule.setStatusCode(200);
        schemaRule.setRequestSchema("{\"type\": \"object\", \"properties\": {\"id\": {\"type\": \"number\"}}, \"required\": [\"id\"]}");
        repository.save(schemaRule);
    }

    private void seedMqRules(MqRuleRepository repository) {
        // MQ Rule 1: Order Processing
        MqRuleEntity rule = new MqRuleEntity();
        rule.setName("Order Processor");
        rule.setInputQueue("ORDER.IN.Q");
        rule.setJsonPathCondition("$.orderType");
        rule.setJsonPathValue("BOOK");
        rule.setResponsePayload("{\"orderId\": \"ORD-{{request.correlationId}}\", \"status\": \"PROCESSED\", \"type\": \"BOOK\"}");
        rule.setDelayMs(1000);
        repository.save(rule);
    }
}
