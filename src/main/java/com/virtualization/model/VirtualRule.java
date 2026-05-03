package com.virtualization.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class VirtualRule {
    private String path;
    private String method;
    private VirtualResponse response;
    
    // For Phase 1, basic matching logic
    public boolean matches(VirtualRequest request) {
        if (this.path != null && !this.path.equals(request.getPath())) {
            return false;
        }
        if (this.method != null && !this.method.equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return true;
    }
}
