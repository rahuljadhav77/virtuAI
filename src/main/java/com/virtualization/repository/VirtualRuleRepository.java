package com.virtualization.repository;

import com.virtualization.entity.VirtualRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VirtualRuleRepository extends JpaRepository<VirtualRuleEntity, Long> {
    List<VirtualRuleEntity> findByMethodIgnoreCase(String method);
}
