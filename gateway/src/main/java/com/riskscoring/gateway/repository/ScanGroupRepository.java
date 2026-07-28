package com.riskscoring.gateway.repository;

import com.riskscoring.gateway.entity.ScanGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScanGroupRepository extends JpaRepository<ScanGroup, UUID> {
}