package com.virtualization.controller;

import com.virtualization.entity.TrafficLogEntity;
import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.repository.TrafficLogRepository;
import com.virtualization.repository.VirtualRuleRepository;
import com.virtualization.service.ProxyService;
import com.virtualization.service.RecorderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recorder")
public class RecorderController {
    private final RecorderService recorderService;
    private final ProxyService proxyService;
    private final TrafficLogRepository logRepository;
    private final VirtualRuleRepository ruleRepository;

    public RecorderController(RecorderService recorderService, 
                              ProxyService proxyService, 
                              TrafficLogRepository logRepository,
                              VirtualRuleRepository ruleRepository) {
        this.recorderService = recorderService;
        this.proxyService = proxyService;
        this.logRepository = logRepository;
        this.ruleRepository = ruleRepository;
    }

    @PostMapping("/start")
    public String start(@RequestParam String targetUrl) {
        proxyService.setTargetBaseUrl(targetUrl);
        recorderService.setRecordingEnabled(true);
        return "Recording started for target: " + targetUrl;
    }

    @PostMapping("/stop")
    public String stop() {
        recorderService.setRecordingEnabled(false);
        return "Recording stopped";
    }

    @GetMapping("/logs")
    public List<TrafficLogEntity> getLogs(@RequestParam(required = false) Long serviceId) {
        if (serviceId != null) {
            return logRepository.findByServiceIdOrderByTimestampDesc(serviceId);
        }
        return logRepository.findAll();
    }

    @PostMapping("/auto-mock")
    public String autoMock() {
        List<TrafficLogEntity> logs = logRepository.findAll();
        for (TrafficLogEntity log : logs) {
            // Check if rule already exists to avoid duplicates
            // Simple check by path and method for now
            VirtualRuleEntity rule = new VirtualRuleEntity();
            rule.setName("Recorded Rule - " + log.getId());
            rule.setPathPattern(log.getPath());
            rule.setMethod(log.getMethod());
            rule.setResponseBody(log.getResponseBody());
            rule.setStatusCode(log.getStatusCode());
            rule.setPriority(-1); // Lower priority for recorded rules
            ruleRepository.save(rule);
        }
        return "Generated " + logs.size() + " mock rules from traffic logs";
    }
}
