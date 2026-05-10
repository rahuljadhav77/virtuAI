package com.virtualization.agent;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final OrchestratorAgent orchestrator = new OrchestratorAgent();
    private final Map<String, Task> taskStore = new HashMap<>();

    @PostMapping("/orchestrate")
    public Map<String, Object> orchestrate(@RequestBody Map<String, String> request) {
        String task = request.getOrDefault("task", "general-check");
        Map<AgentType, AgentResult> results = orchestrator.orchestrate(task);

        String taskId = UUID.randomUUID().toString();
        taskStore.put(taskId, new Task(taskId, task, results));

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("results", results);
        return response;
    }

    @GetMapping("/status/{taskId}")
    public Map<String, Object> getStatus(@PathVariable String taskId) {
        Task task = taskStore.get(taskId);
        if (task == null) {
            return Map.of("error", "Task not found");
        }
        return Map.of(
            "taskId", task.id,
            "status", "completed",
            "results", task.results
        );
    }

    @PostMapping("/delegate")
    public AgentResult delegate(@RequestBody Map<String, String> request) {
        String typeStr = request.get("agentType");
        String task = request.get("task");

        AgentType type = AgentType.valueOf(typeStr.toUpperCase());
        return orchestrator.delegateTo(type, task);
    }

    static class Task {
        String id;
        String description;
        Map<AgentType, AgentResult> results;

        Task(String id, String description, Map<AgentType, AgentResult> results) {
            this.id = id;
            this.description = description;
            this.results = results;
        }
    }
}