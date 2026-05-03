package com.virtualization.model;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class VirtualResponse {
    private int statusCode;
    private Map<String, String> headers;
    private String body;
    private long delayMs;
}
