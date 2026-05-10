package com.virtualization.test;

import com.virtualization.agent.*;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AgentOrchestrationTest {

    @Test
    void orchestrateAllAgents() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        Map<AgentType, AgentResult> results = orchestrator.orchestrate("full-system-analysis");

        assertNotNull(results);
        assertFalse(results.isEmpty());
        System.out.println("Agents executed: " + results.size());
    }

    @Test
    void delegateToSpecificAgent() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        AgentResult result = orchestrator.delegateTo(AgentType.CODE_REVIEWER, "review-code");

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Code Review"));
    }

    @Test
    void selfHealingAgentWorks() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        AgentResult result = orchestrator.delegateTo(AgentType.SELF_HEALING, "health-check");

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Self-Healing"));
    }

    @Test
    void documentationAgentWorks() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        AgentResult result = orchestrator.delegateTo(AgentType.DOCUMENTATION, "generate-docs");

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Documentation"));
    }

    @Test
    void performanceAgentWorks() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        AgentResult result = orchestrator.delegateTo(AgentType.PERFORMANCE_ANALYZER, "analyze-performance");

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Performance"));
    }

    @Test
    void securityScannerAgentWorks() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        AgentResult result = orchestrator.delegateTo(AgentType.SECURITY_SCANNER, "scan-vulnerabilities");

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Security"));
    }

    @Test
    void testGeneratorAgentWorks() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        AgentResult result = orchestrator.delegateTo(AgentType.TEST_GENERATOR, "generate-tests");

        assertTrue(result.isSuccess());
        assertTrue(result.getOutput().contains("Test Generation"));
    }

    @Test
    void allAgentTypesAvailable() {
        OrchestratorAgent orchestrator = new OrchestratorAgent();
        List<AgentType> agents = orchestrator.getAvailableAgents();

        assertEquals(6, agents.size());
        assertTrue(agents.contains(AgentType.SELF_HEALING));
        assertTrue(agents.contains(AgentType.CODE_REVIEWER));
    }

    @Test
    void agentResultMetadataWorks() {
        AgentResult result = new AgentResult(true, "test output");
        assertTrue(result.isSuccess());
        assertEquals("test output", result.getOutput());
        assertNotNull(result.getMetadata());
    }
}