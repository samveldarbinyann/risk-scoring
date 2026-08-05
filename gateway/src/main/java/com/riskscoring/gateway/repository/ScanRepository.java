package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.Scan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScanRepository extends JpaRepository<Scan, UUID> {

    List<Scan> findByGroupId(UUID groupId);

    List<Scan> findByGroupIdIn(List<UUID> groupIds);
}