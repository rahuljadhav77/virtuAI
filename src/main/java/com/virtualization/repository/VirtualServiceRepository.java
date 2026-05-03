package com.virtualization.repository;

import com.virtualization.entity.VirtualServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirtualServiceRepository extends JpaRepository<VirtualServiceEntity, Long> {
}
