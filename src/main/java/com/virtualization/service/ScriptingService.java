package com.virtualization.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ScriptingService {
    public Object execute(String script, Map<String, Object> variables) {
        if (script == null || script.isEmpty()) return null;
        
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
}
