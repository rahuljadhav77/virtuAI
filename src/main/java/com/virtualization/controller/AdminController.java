package com.virtualization.controller;

import com.virtualization.entity.MqRuleEntity;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.entity.VirtualServiceEntity;
import com.virtualization.repository.MqRuleRepository;
import com.virtualization.repository.VirtualRuleRepository;
import com.virtualization.repository.VirtualServiceRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final VirtualServiceRepository serviceRepository;
    private final VirtualRuleRepository httpRuleRepository;
    private final MqRuleRepository mqRuleRepository;

    public AdminController(VirtualServiceRepository serviceRepository, 
                           VirtualRuleRepository httpRuleRepository,
                           MqRuleRepository mqRuleRepository) {
        this.serviceRepository = serviceRepository;
        this.httpRuleRepository = httpRuleRepository;
        this.mqRuleRepository = mqRuleRepository;
    }

    // --- Service Management ---
    @PostMapping("/services")
    public VirtualServiceEntity createService(@RequestBody VirtualServiceEntity service) {
        return serviceRepository.save(service);
    }

    @GetMapping("/services")
    public List<VirtualServiceEntity> listServices() {
        return serviceRepository.findAll();
    }

    @DeleteMapping("/services/{id}")
    public void deleteService(@PathVariable Long id) {
        serviceRepository.deleteById(id);
    }

    // --- HTTP Rule Management ---
    @PostMapping("/rules/http")
    public VirtualRuleEntity createHttpRule(@RequestBody VirtualRuleEntity rule) {
        return httpRuleRepository.save(rule);
    }

    @GetMapping("/rules/http")
    public List<VirtualRuleEntity> listHttpRules() {
        return httpRuleRepository.findAll();
    }

    @PutMapping("/rules/http/{id}")
    public VirtualRuleEntity updateHttpRule(@PathVariable Long id, @RequestBody VirtualRuleEntity updatedRule) {
        return httpRuleRepository.findById(id).map(rule -> {
            rule.setName(updatedRule.getName());
            rule.setPathPattern(updatedRule.getPathPattern());
            rule.setMethod(updatedRule.getMethod());
            rule.setResponseBody(updatedRule.getResponseBody());
            rule.setStatusCode(updatedRule.getStatusCode());
            rule.setResponseHeaders(updatedRule.getResponseHeaders());
            rule.setDelayMs(updatedRule.getDelayMs());
            rule.setJsonPathCondition(updatedRule.getJsonPathCondition());
            rule.setJsonPathValue(updatedRule.getJsonPathValue());
            rule.setPriority(updatedRule.getPriority());
            rule.setRequiredState(updatedRule.getRequiredState());
            rule.setNewState(updatedRule.getNewState());
            rule.setScript(updatedRule.getScript());
            return httpRuleRepository.save(rule);
        }).orElseThrow(() -> new RuntimeException("Rule not found"));
    }

    @DeleteMapping("/rules/http/{id}")
    public void deleteHttpRule(@PathVariable Long id) {
        httpRuleRepository.deleteById(id);
    }

    // --- MQ Rule Management ---
    @PostMapping("/rules/mq")
    public MqRuleEntity createMqRule(@RequestBody MqRuleEntity rule) {
        return mqRuleRepository.save(rule);
    }

    @GetMapping("/rules/mq")
    public List<MqRuleEntity> listMqRules() {
        return mqRuleRepository.findAll();
    }

    @PutMapping("/rules/mq/{id}")
    public MqRuleEntity updateMqRule(@PathVariable Long id, @RequestBody MqRuleEntity updatedRule) {
        return mqRuleRepository.findById(id).map(rule -> {
            rule.setName(updatedRule.getName());
            rule.setInputQueue(updatedRule.getInputQueue());
            rule.setJsonPathCondition(updatedRule.getJsonPathCondition());
            rule.setJsonPathValue(updatedRule.getJsonPathValue());
            rule.setResponsePayload(updatedRule.getResponsePayload());
            rule.setDelayMs(updatedRule.getDelayMs());
            rule.setPriority(updatedRule.getPriority());
            rule.setRequiredState(updatedRule.getRequiredState());
            rule.setNewState(updatedRule.getNewState());
            rule.setScript(updatedRule.getScript());
            return mqRuleRepository.save(rule);
        }).orElseThrow(() -> new RuntimeException("MQ Rule not found"));
    }

    @DeleteMapping("/rules/mq/{id}")
    public void deleteMqRule(@PathVariable Long id) {
        mqRuleRepository.deleteById(id);
    }
}
