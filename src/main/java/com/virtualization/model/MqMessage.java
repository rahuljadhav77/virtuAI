package com.virtualization.model;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class MqMessage {
    private String messageId;
    private String correlationId;
    private String replyTo;
    private String payload;
    private Map<String, String> headers;
    private long timestamp;
}
