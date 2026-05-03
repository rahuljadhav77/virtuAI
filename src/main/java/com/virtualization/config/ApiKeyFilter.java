package com.virtualization.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class ApiKeyFilter implements Filter {
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String VALID_API_KEY = "enterprise-secret";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        
        // Only protect /api/admin paths
        if (path.startsWith("/api/admin")) {
            String apiKey = req.getHeader(API_KEY_HEADER);
            if (apiKey == null || !apiKey.equals(VALID_API_KEY)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\": \"Invalid or missing API Key\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
