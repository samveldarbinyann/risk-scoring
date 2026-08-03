package com.riskscoring.gateway.mapper;

import com.riskscoring.gateway.dto.AlertView;
import com.riskscoring.gateway.repository.AlertRow;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertView toView(AlertRow row) {
        return new AlertView(
                row.id(),
                row.watchlistEntryId(),
                row.address(),
                row.chain(),
                row.previousRiskLevel(),
                row.previousScore(),
                row.newRiskLevel(),
                row.newScore(),
                row.scanId(),
                row.triggeredAt()
        );
    }
}