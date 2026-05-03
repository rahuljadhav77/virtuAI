package com.virtualization.model;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

@Data
@Builder
public class VirtualRequest {
    private String path;
    private String method;
    private Map<String, String> headers;
    private String body;
    private Map<String, String> queryParams;
}
