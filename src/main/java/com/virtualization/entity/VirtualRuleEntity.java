package com.virtualization.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Map;

@Entity
@Table(name = "virtual_rule")
@Data
public class VirtualRuleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String pathPattern;
    private String method;

    @Column(columnDefinition = "TEXT")
    private String responseBody;
    
    private int statusCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rule_headers", joinColumns = @JoinColumn(name = "rule_id"))
    @MapKeyColumn(name = "header_key")
    @Column(name = "header_value")
    private Map<String, String> responseHeaders;

    private long delayMs;

    private String jsonPathCondition;
    private String jsonPathValue;

    private int priority = 0;
    private String requiredState;
    private String newState;
    private Long serviceId;

    @Column(columnDefinition = "TEXT")
    private String script;

    @Column(columnDefinition = "TEXT")
    private String requestSchema;

    @Column(columnDefinition = "TEXT")
    private String responseSchema;
}
