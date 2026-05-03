package com.virtualization.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyFilterTest {

    private static class RecordingFilterChain implements FilterChain {
        boolean called = false;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            called = true;
        }
    }

    @Test
    void shouldRejectAdminPathWhenApiKeyMissing() throws IOException, ServletException {
        ApiKeyFilter filter = new ApiKeyFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertFalse(chain.called);
        assertTrue(response.getContentAsString().contains("Invalid or missing API Key"));
    }

    @Test
    void shouldAllowAdminPathWhenApiKeyValid() throws IOException, ServletException {
        ApiKeyFilter filter = new ApiKeyFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/status");
        request.addHeader("X-API-KEY", "enterprise-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.called);
    }

    @Test
    void shouldAllowNonAdminPathWithoutApiKey() throws IOException, ServletException {
        ApiKeyFilter filter = new ApiKeyFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/info");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.called);
    }
}
