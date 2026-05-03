package com.virtualization.service;

import com.virtualization.model.VirtualRequest;
import com.virtualization.model.VirtualResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProxyService {
    private final RestTemplate restTemplate = new RestTemplate();
    private String targetBaseUrl = null;

    public void setTargetBaseUrl(String url) {
        this.targetBaseUrl = url;
    }

    public String getTargetBaseUrl() {
        return targetBaseUrl;
    }

    public VirtualResponse proxyRequest(VirtualRequest request) {
        if (targetBaseUrl == null) {
            return null;
        }

        String url = targetBaseUrl + request.getPath();
        HttpHeaders headers = new HttpHeaders();
        if (request.getHeaders() != null) {
            // Filter out hop-by-hop headers if needed, but for MVP keep it simple
            request.getHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("host") && !k.equalsIgnoreCase("content-length")) {
                    headers.add(k, v);
                }
            });
        }

        HttpEntity<String> entity = new HttpEntity<>(request.getBody(), headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    String.class
            );

            Map<String, String> respHeaders = new HashMap<>();
            response.getHeaders().forEach((k, v) -> respHeaders.put(k, String.join(",", v)));

            return VirtualResponse.builder()
                    .statusCode(response.getStatusCode().value())
                    .body(response.getBody())
                    .headers(respHeaders)
                    .build();
        } catch (Exception e) {
            return VirtualResponse.builder()
                    .statusCode(502)
                    .body("{\"error\": \"Proxy error: " + e.getMessage() + "\"}")
                    .build();
        }
    }
}
