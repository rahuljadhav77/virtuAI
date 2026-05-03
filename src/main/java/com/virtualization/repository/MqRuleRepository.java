package com.virtualization.repository;

import com.virtualization.entity.MqRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MqRuleRepository extends JpaRepository<MqRuleEntity, Long> {
    List<MqRuleEntity> findByInputQueue(String inputQueue);
}
