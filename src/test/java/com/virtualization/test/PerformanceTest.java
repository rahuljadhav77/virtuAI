package com.virtualization.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void concurrentRequestsHandled() throws InterruptedException, java.util.concurrent.ExecutionException {
        int threadCount = 20;
        int requestsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            Future<Integer> future = executor.submit(() -> {
                int successCount = 0;
                for (int j = 0; j < requestsPerThread; j++) {
                    ResponseEntity<String> response = restTemplate.getForEntity(
                        baseUrl() + "/actuator/health",
                        String.class
                    );
                    if (response.getStatusCode().is2xxSuccessful()) {
                        successCount++;
                    }
                }
                return successCount;
            });
            futures.add(future);
        }

        int totalSuccess = 0;
        for (Future<Integer> future : futures) {
            totalSuccess += future.get();
        }

        long duration = System.currentTimeMillis() - startTime;
        int totalRequests = threadCount * requestsPerThread;

        System.out.println("Performance: " + totalRequests + " requests in " + duration + "ms");
        System.out.println("Throughput: " + (totalRequests * 1000.0 / duration) + " req/sec");
        System.out.println("Success rate: " + (totalSuccess * 100.0 / totalRequests) + "%");

        assertEquals(totalRequests, totalSuccess, "All requests should succeed");
        assertTrue(duration < 30000, "Should complete within 30 seconds");
    }

    @Test
    void responseTimeUnderLoad() {
        Map<String, Object> service = new HashMap<>();
        service.put("name", "Perf Test Service");
        service.put("type", "HTTP");

        restTemplate.postForEntity(
            baseUrl() + "/api/admin/services",
            new HttpEntity<>(service, createHeaders()),
            String.class
        );

        Map<String, Object> rule = new HashMap<>();
        rule.put("name", "Perf Rule");
        rule.put("pathPattern", "/perf-test");
        rule.put("method", "GET");
        rule.put("responseBody", "{\"result\": \"ok\"}");
        rule.put("statusCode", 200);

        restTemplate.postForEntity(
            baseUrl() + "/api/admin/rules",
            new HttpEntity<>(rule, createHeaders()),
            String.class
        );

        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            long start = System.nanoTime();
            restTemplate.getForEntity(baseUrl() + "/perf-test", String.class);
            long time = (System.nanoTime() - start) / 1_000_000;
            responseTimes.add(time);
        }

        double avg = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double max = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double p95 = responseTimes.stream().sorted().skip((long)(responseTimes.size() * 0.95)).findFirst().orElse(0L);

        System.out.println("Avg response time: " + avg + "ms");
        System.out.println("Max response time: " + max + "ms");
        System.out.println("P95 response time: " + p95 + "ms");

        assertTrue(avg < 500, "Average response time should be under 500ms");
        assertTrue(p95 < 1000, "P95 response time should be under 1 second");
    }

    @Test
    void memoryUsageStaysStable() {
        Runtime runtime = Runtime.getRuntime();

        System.gc();
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

        for (int i = 0; i < 100; i++) {
            restTemplate.getForEntity(baseUrl() + "/actuator/health", String.class);
            restTemplate.getForEntity(baseUrl() + "/actuator/metrics", String.class);
        }

        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = afterMemory - beforeMemory;

        System.out.println("Memory increase after 100 requests: " + (memoryIncrease / 1024) + "KB");

        assertTrue(memoryIncrease < 100 * 1024 * 1024, "Memory should not increase by more than 100MB");
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", "enterprise-secret");
        return headers;
    }
}