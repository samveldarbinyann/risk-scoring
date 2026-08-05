package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.ScanGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScanGroupRepository extends JpaRepository<ScanGroup, UUID> {

    List<ScanGroup> findTop5ByUserIdOrderByRequestedAtDesc(UUID userId);
}