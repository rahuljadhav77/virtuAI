package com.virtualization.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StateService {
    
    private final StringRedisTemplate redisTemplate;
    private final Map<String, String> fallbackState = new ConcurrentHashMap<>();

    public StateService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setState(String contextId, String key, String value) {
        String fullKey = "state:" + contextId + ":" + key;
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(fullKey, value);
                return;
            } catch (Exception ignored) {}
        }
        fallbackState.put(fullKey, value);
    }

    public String getState(String contextId, String key) {
        String fullKey = "state:" + contextId + ":" + key;
        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(fullKey);
                if (value != null) return value;
            } catch (Exception ignored) {}
        }
        return fallbackState.getOrDefault(fullKey, "");
    }

    public void clearState(String contextId) {
        String prefix = "state:" + contextId + ":";
        if (redisTemplate != null) {
            try {
                java.util.Set<String> keys = redisTemplate.keys(prefix + "*");
                if (keys != null) redisTemplate.delete(keys);
            } catch (Exception ignored) {}
        }
        fallbackState.keySet().removeIf(k -> k.startsWith(prefix));
    }
}
