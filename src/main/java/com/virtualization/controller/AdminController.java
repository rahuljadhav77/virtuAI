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

    @PutMapping("/services/{id}/stop")
    public VirtualServiceEntity stopService(@PathVariable Long id) {
        VirtualServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        service.setEnabled(false);
        return serviceRepository.save(service);
    }

    @PutMapping("/services/{id}/start")
    public VirtualServiceEntity startService(@PathVariable Long id) {
        VirtualServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        service.setEnabled(true);
        return serviceRepository.save(service);
    }
    @PostMapping("/rules/http")
    public VirtualRuleEntity createHttpRule(@RequestBody VirtualRuleEntity rule) {
        return httpRuleRepository.save(rule);
    }

    @GetMapping("/rules/http")
    public List<VirtualRuleEntity> listHttpRules() {
        return httpRuleRepository.findAll();
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
}
