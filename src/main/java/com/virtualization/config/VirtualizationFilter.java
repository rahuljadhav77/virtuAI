package com.virtualization.config;

import com.virtualization.engine.RuleEngine;
import com.virtualization.model.VirtualRequest;
import com.virtualization.model.VirtualResponse;
import com.virtualization.service.ProxyService;
import com.virtualization.service.RecorderService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Order(2) // After ApiKeyFilter
public class VirtualizationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(VirtualizationFilter.class);
    private final RuleEngine ruleEngine;
    private final ProxyService proxyService;
    private final RecorderService recorderService;

    public VirtualizationFilter(RuleEngine ruleEngine, ProxyService proxyService, RecorderService recorderService) {
        this.ruleEngine = ruleEngine;
        this.proxyService = proxyService;
        this.recorderService = recorderService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Skip system paths (API, static files, actuator)
        if (path.startsWith("/api/") || path.startsWith("/actuator/") ||
            path.equals("/") || path.equals("/index.html") || path.equals("/index.css") ||
            path.startsWith("/h2-console") || path.contains(".")) {
            chain.doFilter(request, response);
            return;
        }

        log.info("Intercepting request: {} {}", req.getMethod(), path);

        // Normalize Request
        String body = "";
        if ("POST".equalsIgnoreCase(req.getMethod()) || "PUT".equalsIgnoreCase(req.getMethod()) ||
            "PATCH".equalsIgnoreCase(req.getMethod())) {
            body = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        }

        VirtualRequest virtualRequest = normalizeRequest(req, body);

        // Rule Evaluation
        Optional<VirtualResponse> matchedResponse = ruleEngine.evaluate(virtualRequest);

        VirtualResponse vResponse = null;
        if (matchedResponse.isPresent()) {
            vResponse = matchedResponse.get();
        } else if (proxyService.getTargetBaseUrl() != null) {
            vResponse = proxyService.proxyRequest(virtualRequest);
        }

        // Only log traffic when there's a matching rule (not 404s)
        if (vResponse != null && vResponse.getStatusCode() != 404) {
            recorderService.logTraffic(virtualRequest, vResponse);
        }

        if (vResponse != null) {
            if (vResponse.getDelayMs() > 0) {
                try {
                    Thread.sleep(vResponse.getDelayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            res.setStatus(vResponse.getStatusCode());
            if (vResponse.getHeaders() != null) {
                vResponse.getHeaders().forEach(res::setHeader);
            }
            res.getWriter().write(vResponse.getBody());
            return;
        }

        // No matching rule - just pass through (don't log 404s)
        chain.doFilter(request, response);
    }

    private VirtualRequest normalizeRequest(HttpServletRequest request, String body) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }

        Map<String, String> queryParams = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> {
            queryParams.put(key, String.join(",", value));
        });

        return VirtualRequest.builder()
                .path(request.getRequestURI())
                .method(request.getMethod())
                .headers(headers)
                .queryParams(queryParams)
                .body(body)
                .build();
    }
}
