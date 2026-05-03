package com.virtualization.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VirtualizationController {

    @RequestMapping("/health-check")
    public String health() {
        return "UP";
    }
    
    // The main virtualization logic has been moved to VirtualizationFilter
    // to prevent it from intercepting system paths and static resources.
}
