package com.virtualization.controller;

import com.virtualization.model.MqMessage;
import com.virtualization.service.MqVirtualizationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mq")
public class MqAdminController {
    private final MqVirtualizationService mqService;

    public MqAdminController(MqVirtualizationService mqService) {
        this.mqService = mqService;
    }

    @PostMapping("/send/{queueName}")
    public String send(@PathVariable String queueName, @RequestBody MqMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId("REQ-" + System.currentTimeMillis());
        }
        mqService.sendMessage(queueName, message);
        return "Message accepted by virtual queue " + queueName;
    }

    @GetMapping("/receive/{queueName}")
    public MqMessage receive(@PathVariable String queueName, @RequestParam(defaultValue = "5000") long timeout) throws InterruptedException {
        return mqService.receiveMessage(queueName, timeout);
    }
}
