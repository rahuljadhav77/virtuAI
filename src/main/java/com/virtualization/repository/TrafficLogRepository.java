package com.virtualization.repository;

import com.virtualization.entity.TrafficLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrafficLogRepository extends JpaRepository<TrafficLogEntity, Long> {
}
