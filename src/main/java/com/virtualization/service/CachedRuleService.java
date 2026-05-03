package com.virtualization.service;

import com.virtualization.entity.VirtualRuleEntity;
import com.virtualization.repository.VirtualRuleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CachedRuleService {
    private final VirtualRuleRepository repository;

    public CachedRuleService(VirtualRuleRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "http_rules", key = "#method")
    public List<VirtualRuleEntity> getRulesByMethod(String method) {
        return repository.findByMethodIgnoreCase(method);
    }

    @CacheEvict(value = "http_rules", allEntries = true)
    public void evictCache() {
        // Method to manually or programmatically clear cache
    }
}
