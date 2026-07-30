package com.riskscoring.gateway.service.impl;

import com.riskscoring.gateway.dto.AlertView;
import com.riskscoring.gateway.mapper.AlertMapper;
import com.riskscoring.gateway.repository.AlertRepository;
import com.riskscoring.gateway.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final AlertMapper alertMapper;

    @Override
    public List<AlertView> listAlerts(UUID userId) {
        return alertRepository.findAllByUserId(userId).stream()
                .map(alertMapper::toView)
                .toList();
    }
}