package com.virtualization.service;

import com.virtualization.model.VirtualRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScriptingServiceTest {

    private final ScriptingService scriptingService = new ScriptingService();

    @Test
    void safeScriptExecutesSuccessfully() {
        VirtualRequest request = VirtualRequest.builder()
                .path("/safe")
                .method("GET")
                .headers(Map.of())
                .body(null)
                .queryParams(Map.of())
                .build();

        Object result = scriptingService.execute("return 'Hello ' + request.path", Map.of("request", request));

        assertEquals("Hello /safe", result);
    }

    @Test
    void dangerousScriptIsRejected() {
        Object result = scriptingService.execute("Runtime.getRuntime().exec('rm -rf /')", Map.of());

        assertTrue(result instanceof String);
        assertTrue(((String) result).contains("disallowed"));
    }
}
