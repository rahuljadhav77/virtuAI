package com.virtualization.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "virtual_service")
@Data
public class VirtualServiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private boolean enabled = true;
    private String type; // HTTP or MQ
}
