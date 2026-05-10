package com.virtualization.agent;

public enum AgentType {
    ORCHESTRATOR("Main coordinator that routes tasks to specialized agents"),
    CODE_REVIEWER("Reviews code quality and suggests improvements"),
    DOCUMENTATION("Generates and maintains documentation"),
    TEST_GENERATOR("Creates and validates test cases"),
    SELF_HEALING("Monitors and fixes mock discrepancies"),
    PERFORMANCE_ANALYZER("Analyzes system performance and bottlenecks"),
    SECURITY_SCANNER("Identifies security vulnerabilities");

    private final String description;

    AgentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}