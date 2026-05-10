package com.virtualization.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ScriptingService {
    private static final List<String> DISALLOWED_SCRIPT_TOKENS = Arrays.asList(
            "import ",
            "system.",
            "runtime.",
            "java.lang",
            "new java.",
            "new file",
            "new processbuilder",
            "processbuilder",
            "classloader",
            "getclassloader",
            "runtime.getruntime",
            "exec(",
            "socket",
            "serversocket",
            "thread",
            "system.exit",
            "class ",
            "package "
    );

    public Object execute(String script, Map<String, Object> variables) {
        if (script == null || script.isBlank()) return null;

        if (containsDisallowedTokens(script)) {
            return "Error executing script: disallowed operations detected";
        }

        try {
            Binding binding = new Binding();
            if (variables != null) {
                variables.forEach(binding::setVariable);
            }

            GroovyShell shell = new GroovyShell(binding);
            return shell.evaluate(script);
        } catch (Exception e) {
            return "Error executing script: " + e.getMessage();
        }
    }

    private boolean containsDisallowedTokens(String script) {
        String normalized = script.toLowerCase();
        return DISALLOWED_SCRIPT_TOKENS.stream().anyMatch(normalized::contains);
    }
}
