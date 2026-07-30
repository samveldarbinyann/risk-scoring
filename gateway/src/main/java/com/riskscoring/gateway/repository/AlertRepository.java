package com.riskscoring.gateway.repository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository {

    List<AlertRow> findAllByUserId(UUID userId);
}
