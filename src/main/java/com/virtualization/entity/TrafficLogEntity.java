package com.virtualization.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_log")
@Data
public class TrafficLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String method;
    private String path;
    
    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column(columnDefinition = "TEXT")
    private String responseBody;
    
    private int statusCode;
    private LocalDateTime timestamp = LocalDateTime.now();
}
