package com.virtualization.agent;

public class AgentResult {
    private final boolean success;
    private final String output;
    private final java.util.Map<String, Object> metadata;

    public AgentResult(boolean success, String output) {
        this.success = success;
        this.output = output;
        this.metadata = new java.util.HashMap<>();
    }

    public boolean isSuccess() { return success; }
    public String getOutput() { return output; }
    public java.util.Map<String, Object> getMetadata() { return metadata; }
}