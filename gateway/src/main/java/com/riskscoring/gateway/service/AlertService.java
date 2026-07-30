package com.riskscoring.gateway.service;

import com.riskscoring.gateway.dto.AlertView;

import java.util.List;
import java.util.UUID;

public interface AlertService {

    List<AlertView> listAlerts(UUID userId);
}