package com.virtualization.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiConfig {
    private String provider = "gemini";
    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private String model = "gemini-1.5-flash";
    private double temperature = 0.7;
    private int maxTokens = 2000;
    private int timeoutSeconds = 30;
    private boolean enabled = false;
}
