package com.riskscoring.monitor.repository;

import com.riskscoring.monitor.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
}
