package com.virtualization.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "mq_rule")
@Data
public class MqRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String inputQueue;
    
    private String jsonPathCondition;
    private String jsonPathValue;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;
    
    private long delayMs;

    private int priority = 0;
    private String requiredState;
    private String newState;
    private Long serviceId;

    @Column(columnDefinition = "TEXT")
    private String script;
}
